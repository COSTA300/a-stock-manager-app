package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.KioskDatabase
import com.example.data.model.AuditLog
import com.example.data.model.StockUnit
import com.example.data.repository.KioskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val totalInStock: Int = 0,
    val totalSold: Int = 0,
    val totalInRepair: Int = 0,
    val totalRevenue: Double = 0.0
)

class KioskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KioskRepository
    
    // UI state flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("All")
    val selectedBrandFilter = _selectedBrandFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("All")
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _currentOperatorRole = MutableStateFlow("Cashier") // Cashier vs Admin
    val currentOperatorRole = _currentOperatorRole.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    init {
        val database = KioskDatabase.getDatabase(application)
        repository = KioskRepository(database.kioskDao())
    }

    // Live list of all stock units from database
    val allStockUnits: StateFlow<List<StockUnit>> = repository.allStockUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live list of all audit logs
    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Stock Units flow combining multiple criteria
    val filteredStockUnits: StateFlow<List<StockUnit>> = combine(
        allStockUnits,
        _searchQuery,
        _selectedBrandFilter,
        _selectedStatusFilter
    ) { stockList, query, brand, status ->
        stockList.filter { unit ->
            val matchesQuery = query.isBlank() || 
                    unit.imei.contains(query, ignoreCase = true) ||
                    unit.model.contains(query, ignoreCase = true) ||
                    unit.stockId.contains(query, ignoreCase = true)
            
            val matchesBrand = brand == "All" || unit.brand.equals(brand, ignoreCase = true)
            val matchesStatus = status == "All" || unit.status.equals(status, ignoreCase = true)

            matchesQuery && matchesBrand && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics automatically derived from full stock list
    val metrics: StateFlow<DashboardMetrics> = allStockUnits.map { stockList ->
        var inStock = 0
        var sold = 0
        var repair = 0
        var revenue = 0.0
        
        for (unit in stockList) {
            when (unit.status) {
                "In Stock" -> inStock++
                "Sold" -> {
                    sold++
                    revenue += (unit.salePrice ?: 0.0)
                }
                "In Repair" -> repair++
            }
        }
        DashboardMetrics(inStock, sold, repair, revenue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Suggest brand based on IMEI TAC
    fun identifyBrandFromImei(imei: String): String {
        if (imei.length < 3) return ""
        val prefix = imei.take(3)
        return when {
            prefix.startsWith("351") || prefix.startsWith("352") || prefix.startsWith("355") -> "Samsung"
            prefix.startsWith("861") || prefix.startsWith("862") || prefix.startsWith("864") || prefix.startsWith("868") -> "Spectra"
            prefix.startsWith("353") || prefix.startsWith("356") || prefix.startsWith("359") -> "Vivo"
            prefix.startsWith("354") || prefix.startsWith("357") || prefix.startsWith("358") -> "Apple"
            else -> "Other"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
    }

    fun updateStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun toggleOperatorRole() {
        _currentOperatorRole.value = if (_currentOperatorRole.value == "Cashier") "Admin" else "Cashier"
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    // 1. Stock Intake (Receiving)
    fun addStockUnit(imei: String, brand: String, model: String, onSuccess: () -> Unit) {
        if (imei.length != 15 || !imei.all { it.isDigit() }) {
            _uiMessage.value = "Error: IMEI must be exactly 15 digits"
            return
        }

        viewModelScope.launch {
            // Check for duplicate IMEI
            val existing = repository.getStockUnitByImei(imei)
            if (existing != null) {
                _uiMessage.value = "Error: Device with IMEI $imei already exists in stock (Status: ${existing.status})"
                return@launch
            }

            // Create initial placeholder Stock ID, later overwrite with generated serial
            val timestamp = System.currentTimeMillis()
            val initialUnit = StockUnit(
                imei = imei,
                brand = brand,
                model = model,
                status = "In Stock",
                dateAdded = timestamp,
                stockId = "TEMP"
            )

            val insertedId = repository.insertStockUnit(initialUnit)
            
            // Generate clean, formatted stock ID (e.g. KSM-1015)
            val finalStockId = "KSM-${String.format("%04d", 1000 + insertedId)}"
            val updatedUnit = initialUnit.copy(id = insertedId, stockId = finalStockId)
            repository.insertStockUnit(updatedUnit)

            // Log the intake event
            val log = AuditLog(
                imei = imei,
                brand = brand,
                model = model,
                action = "Received",
                timestamp = timestamp,
                remarks = "Received new handset. Stock ID: $finalStockId",
                operatorRole = _currentOperatorRole.value
            )
            repository.insertAuditLog(log)

            _uiMessage.value = "$brand $model added successfully!"
            onSuccess()
        }
    }

    // 2. Clear out all records for clean simulation/testing (Admin command)
    fun clearAllInventory() {
        viewModelScope.launch {
            for (unit in allStockUnits.value) {
                repository.deleteStockUnitById(unit.id)
            }
            _uiMessage.value = "All inventory data successfully cleared"
        }
    }

    // 3. Status Change & Sales Recording
    fun recordSale(unit: StockUnit, price: Double, customerName: String, remark: String?) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val updatedUnit = unit.copy(
                status = "Sold",
                salePrice = price,
                customerName = customerName.ifBlank { "Walk-in Customer" },
                saleDate = timestamp
            )
            repository.insertStockUnit(updatedUnit)

            // Log Sale Event
            val log = AuditLog(
                imei = unit.imei,
                brand = unit.brand,
                model = unit.model,
                action = "Sold",
                timestamp = timestamp,
                remarks = "Sold for $${String.format("%.2f", price)} to ${customerName.ifBlank { "Walk-in" }}. Sales log: ${remark ?: "None"}",
                operatorRole = _currentOperatorRole.value
            )
            repository.insertAuditLog(log)
            _uiMessage.value = "Device ${unit.imei} marked as SOLD!"
        }
    }

    // 4. Send to Repair Tracking
    fun sendToRepair(unit: StockUnit, repairCenter: String, issueDescription: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val updatedUnit = unit.copy(
                status = "In Repair",
                repairSentDate = timestamp,
                repairCenter = repairCenter.ifBlank { "General Repair Service" },
                repairNotes = issueDescription.ifBlank { "Device diagnostics needed" }
            )
            repository.insertStockUnit(updatedUnit)

            // Log Repair Event
            val log = AuditLog(
                imei = unit.imei,
                brand = unit.brand,
                model = unit.model,
                action = "Sent to Repair",
                timestamp = timestamp,
                remarks = "Sent to '$repairCenter'. Issue: $issueDescription",
                operatorRole = _currentOperatorRole.value
            )
            repository.insertAuditLog(log)
            _uiMessage.value = "Device ${unit.imei} sent for Repair!"
        }
    }

    // 5. Return From Repair
    fun returnFromRepair(unit: StockUnit, diagnosticNotes: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val updatedUnit = unit.copy(
                status = "In Stock",
                repairNotes = "Sent issue: ${unit.repairNotes}\nReturned notes: ${diagnosticNotes.ifBlank { "Completed" }}"
            )
            repository.insertStockUnit(updatedUnit)

            // Log Return Event
            val log = AuditLog(
                imei = unit.imei,
                brand = unit.brand,
                model = unit.model,
                action = "Returned from Repair",
                timestamp = timestamp,
                remarks = "Back in stock. Repairs info: ${diagnosticNotes.ifBlank { "Completed" }}",
                operatorRole = _currentOperatorRole.value
            )
            repository.insertAuditLog(log)
            _uiMessage.value = "Device ${unit.imei} back In Stock after repair!"
        }
    }

    // Delete a unit entirely
    fun deleteUnit(id: Long) {
        viewModelScope.launch {
            val unit = repository.getStockUnitById(id)
            if (unit != null) {
                repository.deleteStockUnitById(id)
                repository.insertAuditLog(
                    AuditLog(
                        imei = unit.imei,
                        brand = unit.brand,
                        model = unit.model,
                        action = "Deleted",
                        remarks = "Deleted from system by ${currentOperatorRole.value}"
                    )
                )
                _uiMessage.value = "Stock entry deleted successfully."
            }
        }
    }

    // Export Data CSV Generator
    fun generateStockCsv(): String {
        val list = allStockUnits.value
        val sb = StringBuilder()
        sb.append("StockID,IMEI,Brand,Model,Status,DateAdded,SalePrice,Customer,SaleDate,RepairNotes\n")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        for (item in list) {
            val added = sdf.format(java.util.Date(item.dateAdded))
            val soldDate = item.saleDate?.let { sdf.format(java.util.Date(it)) } ?: ""
            val priceStr = item.salePrice?.toString() ?: ""
            val customer = item.customerName?.replace(",", ";") ?: ""
            val repairNotesEscaped = item.repairNotes?.replace("\n", " ")?.replace(",", ";") ?: ""
            
            sb.append("${item.stockId},${item.imei},${item.brand},${item.model},${item.status},$added,$priceStr,$customer,$soldDate,$repairNotesEscaped\n")
        }
        return sb.toString()
    }
}
