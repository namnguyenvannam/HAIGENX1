package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MiningRecordEntity::class, TradeRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun miningRecordDao(): MiningRecordDao
    abstract fun tradeRecordDao(): TradeRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trade_records` (
                        `id` TEXT NOT NULL,
                        `direction` TEXT NOT NULL,
                        `leverage` INTEGER NOT NULL,
                        `entryFeeHx1` REAL NOT NULL,
                        `entryPriceUsdt` REAL NOT NULL,
                        `exitPriceUsdt` REAL NOT NULL,
                        `outcome` TEXT NOT NULL,
                        `payoutHx1` REAL NOT NULL,
                        `netPnlHx1` REAL NOT NULL,
                        `timestampMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "haigenx1_miner_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
