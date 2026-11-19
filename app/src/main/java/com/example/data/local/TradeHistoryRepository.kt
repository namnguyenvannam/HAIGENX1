package com.example.data.local

import kotlinx.coroutines.flow.Flow

class TradeHistoryRepository(private val dao: TradeRecordDao) {
    val recentTrades: Flow<List<TradeRecordEntity>> = dao.getRecentTrades()

    suspend fun logTrade(record: TradeRecordEntity) {
        dao.insertTrade(record)
    }

    suspend fun upsertCloudTrades(records: List<TradeRecordEntity>) {
        if (records.isNotEmpty()) {
            dao.insertTrades(records)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
