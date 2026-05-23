package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditLog
import com.example.data.model.StockUnit

@Database(entities = [StockUnit::class, AuditLog::class], version = 2, exportSchema = false)
abstract class KioskDatabase : RoomDatabase() {
    abstract fun kioskDao(): KioskDao

    companion object {
        @Volatile
        private var INSTANCE: KioskDatabase? = null

        fun getDatabase(context: Context): KioskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KioskDatabase::class.java,
                    "kiosk_inventory_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
