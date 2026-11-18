package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MiningSourceType {
    MANUAL,
    AUTO,
    BOOST_BONUS
}

@Entity(tableName = "mining_records")
data class MiningRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountHx1: Double,
    val sourceType: String,
    val hashrateSnapshot: Double,
    val timestampMillis: Long = System.currentTimeMillis()
)
