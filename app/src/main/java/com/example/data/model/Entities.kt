package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_units")
data class StockUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imei: String,
    val brand: String,
    val model: String,
    val status: String, // "In Stock", "Sold", "In Repair"
    val dateAdded: Long = System.currentTimeMillis(),
    val stockId: String, // e.g. "KSM-00001" or generated serial
    
    // Sales Details
    val salePrice: Double? = null,
    val customerName: String? = null,
    val saleDate: Long? = null,
    
    // Repair Details
    val repairSentDate: Long? = null,
    val repairCenter: String? = null,
    val repairNotes: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imei: String,
    val brand: String,
    val model: String,
    val action: String, // e.g., "Received", "Sold", "Sent to Repair", "Returned from Repair"
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String? = null,
    val operatorRole: String = "Cashier" // optional role based tracking
)
