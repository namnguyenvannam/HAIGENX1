package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MiningRecordDao {
    @Query("SELECT * FROM mining_records ORDER BY timestampMillis DESC LIMIT 100")
    fun getRecentRecords(): Flow<List<MiningRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MiningRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<MiningRecordEntity>)

    @Query("DELETE FROM mining_records")
    suspend fun clearAll()
}
