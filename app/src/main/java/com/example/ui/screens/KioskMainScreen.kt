package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AuditLog
import com.example.data.model.StockUnit
import com.example.ui.viewmodel.KioskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskMainScreen(
    viewModel: KioskViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab = remember { mutableStateOf(0) } // 0: Dashboard, 1: Stock, 2: Intake, 3: Audit Logs
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val operatorRole by viewModel.currentOperatorRole.collectAsStateWithLifecycle()

    // Dialog state for item detail
    val selectedUnitForDetail = remember { mutableStateOf<StockUnit?>(null) }

    // Display Messages/Toasts
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KM",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kiosk Stock Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kiosk Outlet #104",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    // Export CSV Button
                    IconButton(
                        onClick = {
                            val csvData = viewModel.generateStockCsv()
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Kiosk Stock CSV", csvData)
                            clipboardManager.setPrimaryClip(clip)
                            viewModel.showMessage("📋 CSV structure copied to Clipboard!")
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Role Badge Switcher
                    Card(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { viewModel.toggleOperatorRole() }
                            .testTag("role_toggle_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (operatorRole == "Admin") 
                                MaterialTheme.colorScheme.errorContainer 
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (operatorRole == "Admin") MaterialTheme.colorScheme.error 
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = operatorRole,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (operatorRole == "Admin") 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab.value == 0,
                    onClick = { currentTab.value = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab.value == 1,
                    onClick = { currentTab.value = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    modifier = Modifier.testTag("nav_inventory")
                )
                NavigationBarItem(
                    selected = currentTab.value == 2,
                    onClick = { currentTab.value = 2 },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Stock Intake") },
                    label = { Text("Intake") },
                    modifier = Modifier.testTag("nav_intake")
                )
                NavigationBarItem(
                    selected = currentTab.value == 3,
                    onClick = { currentTab.value = 3 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Audit Logs") },
                    label = { Text("Audit") },
                    modifier = Modifier.testTag("nav_audit")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab.value) {
                0 -> DashboardTab(viewModel) { currentTab.value = 1 }
                1 -> InventoryTab(viewModel, onSelectUnit = { selectedUnitForDetail.value = it })
                2 -> IntakeTab(viewModel) { currentTab.value = 1 }
                3 -> AuditLogTab(viewModel)
            }

            // Global Details & Status Update Dialog Modal
            selectedUnitForDetail.value?.let { unit ->
                DeviceDetailsDialog(
                    unit = unit,
                    viewModel = viewModel,
                    onDismiss = { selectedUnitForDetail.value = null }
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 0: DASHBOARD & SUMMARY METRICS
// -----------------------------------------------------------------
@Composable
fun DashboardTab(
    viewModel: KioskViewModel,
    onNavigateToInventory: () -> Unit
) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val allUnits by viewModel.allStockUnits.collectAsStateWithLifecycle()
    val role by viewModel.currentOperatorRole.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Good day, ${role}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Store is active. Stock adjustments are synced to the local secure database instantly. Standard transactions take under 3 clicks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Dashboard Indicators (Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "In Stock",
                        value = "${metrics.totalInStock} Units",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Sold",
                        value = "${metrics.totalSold} Units",
                        color = Color(0xFF2ECC71),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "In Repair",
                        value = "${metrics.totalInRepair} Units",
                        color = Color(0xFFF1C40F),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Today's Sales",
                        value = "R${String.format(Locale.US, "%.2f", metrics.todayRevenue)}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stock Distribution Chart Representing Brands
        item {
            val brandCounts = allUnits.groupBy { it.brand }
            val inStockUnits = allUnits.filter { it.status == "In Stock" }
            val brandCountsInStock = inStockUnits.groupBy { it.brand }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available Brands in Outlet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (inStockUnits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active stock available. Swipe to Intake to add stock.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val brands = inStockUnits.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
                        brands.forEach { brand ->
                            val count = brandCountsInStock[brand]?.size ?: 0
                            val faction = if (inStockUnits.isEmpty()) 0f else count.toFloat() / inStockUnits.size
                            
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = brand,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$count qty (${(faction * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = faction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (brand == "Spectra") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Everyday Sales Record (resets daily but retains everyday sales history)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Everyday Sales Record",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Total: R${String.format(Locale.US, "%.2f", metrics.totalRevenue)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (metrics.dailySalesHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No sales registered yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        metrics.dailySalesHistory.forEach { (date, dailyRevenue) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "R${String.format(Locale.US, "%.2f", dailyRevenue)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2ECC71)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Outlets Access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onNavigateToInventory,
                    modifier = Modifier.fillMaxWidth().testTag("view_inventory_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View & Filter Raw Handsets")
                }
                
                if (role == "Admin" && allUnits.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearAllInventory() },
                        modifier = Modifier.fillMaxWidth().testTag("admin_clear_all_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Simulation Database")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -----------------------------------------------------------------
// TAB 1: SEARCHABLE/FILTERABLE INVENTORY
// -----------------------------------------------------------------
@Composable
fun InventoryTab(
    viewModel: KioskViewModel,
    onSelectUnit: (StockUnit) -> Unit
) {
    val items by viewModel.filteredStockUnits.collectAsStateWithLifecycle()
    val allUnits by viewModel.allStockUnits.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeBrandFilter by viewModel.selectedBrandFilter.collectAsStateWithLifecycle()
    val activeStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()

    val brandsList = remember(allUnits) {
        val uniqueBrands = allUnits.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + uniqueBrands
    }
    val statusList = listOf("All", "In Stock", "Sold", "In Repair")

    LaunchedEffect(brandsList) {
        if (activeBrandFilter != "All" && !brandsList.contains(activeBrandFilter)) {
            viewModel.updateBrandFilter("All")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = { Text("Search by IMEI, Stock ID, Model...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Brand Filters scroll row
        Text("Filter by Brand:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            brandsList.forEach { brand ->
                FilterChip(
                    selected = activeBrandFilter == brand,
                    onClick = { viewModel.updateBrandFilter(brand) },
                    label = { Text(brand) },
                    modifier = Modifier.testTag("brand_chip_$brand")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status filter row
        Text("Filter by Status:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statusList.forEach { status ->
                FilterChip(
                    selected = activeStatusFilter == status,
                    onClick = { viewModel.updateStatusFilter(status) },
                    label = { Text(status) },
                    modifier = Modifier.testTag("status_chip_${status.replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Devices List
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No handset matches current filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("inventory_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { unit ->
                    HandsetItemCard(unit = unit, onClick = { onSelectUnit(unit) })
                }
            }
        }
    }
}

@Composable
fun HandsetItemCard(
    unit: StockUnit,
    onClick: () -> Unit
) {
    val statusColor = when (unit.status) {
        "In Stock" -> Color(0xFF007A87)
        "Sold" -> Color(0xFF2ECC71)
        "In Repair" -> Color(0xFFE74C3C)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("handset_card_${unit.imei}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Icon Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unit.brand.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = unit.model,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit.stockId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "IMEI: ${unit.imei}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    unit.price?.let {
                        Text(
                            text = "•  R${String.format(Locale.US, "%.2f", it)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = unit.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 2: INTAKE HANDSET
// -----------------------------------------------------------------
@Composable
fun IntakeTab(
    viewModel: KioskViewModel,
    onSuccess: () -> Unit
) {
    var imei by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }

    val brandingOptions = listOf("Spectra", "Samsung", "Vivo", "Honour", "Other")
    var selectedBrand by remember { mutableStateOf("Spectra") }
    var customBrand by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Rapid Stock Intake",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Provision new units securely. Choose the handset brand, model and standard pricing detail below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Intake Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // IMEI Number Input
                    Column {
                        Text(
                            text = "IMEI Number (15-digits)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = imei,
                            onValueChange = { input ->
                                if (input.length <= 15 && input.all { it.isDigit() }) {
                                    imei = input
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("intake_imei_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Enter 15 digit IMEI") },
                            singleLine = true
                        )
                    }

                    // Brand Selection Instead of Auto Detection
                    Column {
                        Text(
                            text = "Select Device Brand",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            brandingOptions.forEach { b ->
                                FilterChip(
                                    selected = selectedBrand == b,
                                    onClick = { selectedBrand = b },
                                    label = { Text(b) },
                                    modifier = Modifier.testTag("intake_brand_chip_$b")
                                )
                            }
                        }
                    }

                    // If "Other" brand is selected, show custom brand name input
                    if (selectedBrand == "Other") {
                        Column {
                            Text(
                                text = "Custom Brand Name",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customBrand,
                                onValueChange = { customBrand = it },
                                modifier = Modifier.fillMaxWidth().testTag("intake_custom_brand_input"),
                                placeholder = { Text("e.g. Apple, Xiaomi, Huawei") },
                                singleLine = true
                            )
                        }
                    }

                    // Model Name Input
                    Column {
                        Text(
                            text = "Model/Variant Description",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            modifier = Modifier.fillMaxWidth().testTag("intake_model_input"),
                            placeholder = { Text("e.g. Spectra G40, Galaxy S24 Ultra") },
                            singleLine = true
                        )
                    }

                    // Price Input Field - South African Rands label
                    Column {
                        Text(
                            text = "Standard Handset Price (R)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null || input.all { it.isDigit() || it == '.' }) {
                                    priceInput = input
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("intake_price_input"),
                            placeholder = { Text("e.g. 4999.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Receive Unit Button (Intake)
                    Button(
                        onClick = {
                            if (imei.length != 15) {
                                viewModel.showMessage("Invalid input: IMEI must be 15 digits.")
                                return@Button
                            }
                            if (model.isBlank()) {
                                viewModel.showMessage("Invalid input: Please type device model.")
                                return@Button
                            }
                            val brandToSubmit = if (selectedBrand == "Other") {
                                customBrand.ifBlank { "Other" }
                            } else {
                                selectedBrand
                            }
                            val priceValue = priceInput.toDoubleOrNull()
                            viewModel.addStockUnit(
                                imei = imei,
                                brand = brandToSubmit,
                                model = model,
                                price = priceValue
                            ) {
                                // On Success, reset form fields
                                imei = ""
                                model = ""
                                priceInput = ""
                                selectedBrand = "Spectra"
                                customBrand = ""
                                onSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_intake_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Kiosk Stock Catalog")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 3: AUDIT LOG TIMELINE
// -----------------------------------------------------------------
@Composable
fun AuditLogTab(
    viewModel: KioskViewModel
) {
    val logs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    var searchLogQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, searchLogQuery) {
        if (searchLogQuery.isBlank()) {
            logs
        } else {
            logs.filter {
                it.imei.contains(searchLogQuery, ignoreCase = true) ||
                it.action.contains(searchLogQuery, ignoreCase = true) ||
                it.model.contains(searchLogQuery, ignoreCase = true) ||
                (it.remarks ?: "").contains(searchLogQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kiosk Outlet Audit Stream",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.Refresh, contentDescription = "Active logging", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Log search field
        OutlinedTextField(
            value = searchLogQuery,
            onValueChange = { searchLogQuery = it },
            placeholder = { Text("Filter logs by IMEI, action or keyword...") },
            modifier = Modifier.fillMaxWidth().testTag("log_search_field"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log trails captured in the database yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("audit_logs_list")
            ) {
                items(filteredLogs) { log ->
                    AuditLogItem(log = log, sdf = sdf)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog, sdf: SimpleDateFormat) {
    val actionColor = when (log.action) {
        "Received" -> Color(0xFF007A87)
        "Sold" -> Color(0xFF2ECC71)
        "Sent to Repair" -> Color(0xFFE74C3C)
        "Returned from Repair" -> Color(0xFF9B59B6)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(actionColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = actionColor
                )
            }
            Text(
                text = sdf.format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${log.brand} ${log.model} (IMEI: ${log.imei})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        log.remarks?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Tracked by: ${log.operatorRole}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

// -----------------------------------------------------------------
// DEVICE DETAILS & ACTION COMPOSABLE DIALOG MODAL
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsDialog(
    unit: StockUnit,
    viewModel: KioskViewModel,
    onDismiss: () -> Unit
) {
    var subActionState by remember { mutableStateOf("") } // "", "SELL", "REPAIR", "RETURN_REPAIR"
    val operatorRole by viewModel.currentOperatorRole.collectAsStateWithLifecycle()

    // Form inputs
    var salePriceInput by remember { mutableStateOf(unit.price?.toString() ?: "") }
    var customerNameInput by remember { mutableStateOf("") }
    var repairCenterInput by remember { mutableStateOf("") }
    var issueNotesInput by remember { mutableStateOf("") }
    var repairResolutionInput by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("device_details_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Title Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${unit.brand} ${unit.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stock ID: ${unit.stockId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Metadata list
                Text(
                    text = "IMEI: ${unit.imei}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Arrival Date: ${sdf.format(Date(unit.dateAdded))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Text(
                    text = "Current Status: ${unit.status}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when (unit.status) {
                        "In Stock" -> Color(0xFF007A87)
                        "Sold" -> Color(0xFF2ECC71)
                        "In Repair" -> Color(0xFFE74C3C)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                unit.price?.let {
                    Text(
                        text = "Intake Price: R${String.format(Locale.US, "%.2f", it)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Render detail subsections depending on static states
                if (unit.status == "Sold") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Sales Record", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Sold To: ${unit.customerName}", style = MaterialTheme.typography.bodySmall)
                            Text("Selling Price: R${unit.salePrice}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            unit.saleDate?.let {
                                Text("Sale Date: ${sdf.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (unit.status == "In Repair") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Repair Record", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Repair Center: ${unit.repairCenter}", style = MaterialTheme.typography.bodySmall)
                            Text("Reported Issue: ${unit.repairNotes}", style = MaterialTheme.typography.bodySmall)
                            unit.repairSentDate?.let {
                                Text("Sent Date: ${sdf.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-action input form transitions (under 3 clicks flow)
                when (subActionState) {
                    "" -> {
                        // Render standard action transition triggers
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (unit.status == "In Stock") {
                                Button(
                                    onClick = { subActionState = "SELL" },
                                    modifier = Modifier.fillMaxWidth().testTag("sell_action_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mark Handset Sold")
                                }
                                Button(
                                    onClick = { subActionState = "REPAIR" },
                                    modifier = Modifier.fillMaxWidth().testTag("repair_action_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send to Repair Center")
                                }
                            }

                            if (unit.status == "In Repair") {
                                Button(
                                    onClick = { subActionState = "RETURN_REPAIR" },
                                    modifier = Modifier.fillMaxWidth().testTag("return_repair_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A87))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Record Return From Repair")
                                }
                            }

                            if (operatorRole == "Admin") {
                                Button(
                                    onClick = {
                                        viewModel.deleteUnit(unit.id)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("delete_item_btn_admin"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Stock Record permanently")
                                }
                            }
                        }
                    }

                    "SELL" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Record Sale Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            
                            OutlinedTextField(
                                value = salePriceInput,
                                onValueChange = { salePriceInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("sell_price_input"),
                                label = { Text("Selling Price (R)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customerNameInput,
                                onValueChange = { customerNameInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("sell_customer_input"),
                                label = { Text("Customer Name / Reference") },
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { subActionState = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val price = salePriceInput.toDoubleOrNull()
                                        if (price == null || price <= 0) {
                                            viewModel.showMessage("Invalid selling price amount.")
                                            return@Button
                                        }
                                        viewModel.recordSale(
                                            unit = unit,
                                            price = price,
                                            customerName = customerNameInput,
                                            remark = "Sold direct kiosk handset"
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("submit_sell_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                                ) {
                                    Text("Confirm Sale")
                                }
                            }
                        }
                    }

                    "REPAIR" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Dispatch to Repair Diagnostics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            
                            OutlinedTextField(
                                value = repairCenterInput,
                                onValueChange = { repairCenterInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("repair_center_input"),
                                label = { Text("Repair Center Name") },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = issueNotesInput,
                                onValueChange = { issueNotesInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("repair_issue_input"),
                                label = { Text("Issue / Defect Description") }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { subActionState = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (issueNotesInput.isBlank()) {
                                            viewModel.showMessage("Please state the issue description.")
                                            return@Button
                                        }
                                        viewModel.sendToRepair(
                                            unit = unit,
                                            repairCenter = repairCenterInput,
                                            issueDescription = issueNotesInput
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("submit_repair_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                                ) {
                                    Text("Confirm Dispatch")
                                }
                            }
                        }
                    }

                    "RETURN_REPAIR" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Log Diagnostic Return Resolution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            
                            OutlinedTextField(
                                value = repairResolutionInput,
                                onValueChange = { repairResolutionInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("repair_resolution_input"),
                                label = { Text("Resolution Notes & Swaps") },
                                placeholder = { Text("e.g. Cleared. Battery replaced.") }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { subActionState = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        viewModel.returnFromRepair(
                                            unit = unit,
                                            diagnosticNotes = repairResolutionInput
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("submit_return_repair_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A87))
                                ) {
                                    Text("Return to Stock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
