package com.example.data.local

import kotlinx.coroutines.flow.Flow

class MiningHistoryRepository(private val dao: MiningRecordDao) {
    val recentRecords: Flow<List<MiningRecordEntity>> = dao.getRecentRecords()

    suspend fun logMining(
        amountHx1: Double,
        sourceType: MiningSourceType,
        hashrateSnapshot: Double
    ) {
        if (amountHx1 <= 0.0) return
        dao.insertRecord(
            MiningRecordEntity(
                amountHx1 = amountHx1,
                sourceType = sourceType.name,
                hashrateSnapshot = hashrateSnapshot,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
