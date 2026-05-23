package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLog
import com.example.data.model.StockUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface KioskDao {
    // StockUnit operations
    @Query("SELECT * FROM stock_units ORDER BY dateAdded DESC")
    fun getAllStockUnits(): Flow<List<StockUnit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockUnit(unit: StockUnit): Long

    @Update
    suspend fun updateStockUnit(unit: StockUnit)

    @Query("SELECT * FROM stock_units WHERE id = :id")
    suspend fun getStockUnitById(id: Long): StockUnit?

    @Query("SELECT * FROM stock_units WHERE imei = :imei")
    suspend fun getStockUnitByImei(imei: String): StockUnit?

    @Query("DELETE FROM stock_units WHERE id = :id")
    suspend fun deleteStockUnitById(id: Long)

    // AuditLog operations
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs WHERE imei = :imei ORDER BY timestamp DESC")
    fun getLogsForImei(imei: String): Flow<List<AuditLog>>
}
