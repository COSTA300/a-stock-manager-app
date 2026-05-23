package com.example.data.repository

import com.example.data.database.KioskDao
import com.example.data.model.AuditLog
import com.example.data.model.StockUnit
import kotlinx.coroutines.flow.Flow

class KioskRepository(private val kioskDao: KioskDao) {

    val allStockUnits: Flow<List<StockUnit>> = kioskDao.getAllStockUnits()
    val allAuditLogs: Flow<List<AuditLog>> = kioskDao.getAllAuditLogs()

    suspend fun insertStockUnit(unit: StockUnit): Long {
        return kioskDao.insertStockUnit(unit)
    }

    suspend fun updateStockUnit(unit: StockUnit) {
        kioskDao.updateStockUnit(unit)
    }

    suspend fun getStockUnitById(id: Long): StockUnit? {
        return kioskDao.getStockUnitById(id)
    }

    suspend fun getStockUnitByImei(imei: String): StockUnit? {
        return kioskDao.getStockUnitByImei(imei)
    }

    suspend fun deleteStockUnitById(id: Long) {
        kioskDao.deleteStockUnitById(id)
    }

    suspend fun insertAuditLog(log: AuditLog) {
        kioskDao.insertAuditLog(log)
    }

    fun getLogsForImei(imei: String): Flow<List<AuditLog>> {
        return kioskDao.getLogsForImei(imei)
    }
}
