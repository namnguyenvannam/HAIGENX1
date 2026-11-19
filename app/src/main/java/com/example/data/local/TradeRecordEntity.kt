package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TradeDirection {
    BUY,
    SHORT
}

enum class TradeOutcome {
    WIN,
    LOSS,
    TIE
}

@Entity(tableName = "trade_records")
data class TradeRecordEntity(
    @PrimaryKey val id: String,
    val direction: String,
    val leverage: Int,
    val entryFeeHx1: Double,
    val entryPriceUsdt: Double,
    val exitPriceUsdt: Double,
    val outcome: String,
    val payoutHx1: Double,
    val netPnlHx1: Double,
    val timestampMillis: Long = System.currentTimeMillis()
)
