package com.overtime.worker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkRecordDao {
    @Query("SELECT * FROM work_records ORDER BY workDate DESC")
    fun observeAll(): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_records WHERE workDate LIKE :month || '%' ORDER BY workDate DESC")
    fun observeMonth(month: String): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_records WHERE workDate LIKE :month || '%' AND (note LIKE '%' || :query || '%' OR workDate LIKE '%' || :query || '%') ORDER BY workDate DESC")
    fun searchMonth(month: String, query: String): Flow<List<WorkRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: WorkRecordEntity)

    @Update
    suspend fun update(record: WorkRecordEntity)

    @Delete
    suspend fun delete(record: WorkRecordEntity)

    @Query("DELETE FROM work_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM work_records ORDER BY workDate DESC")
    suspend fun snapshot(): List<WorkRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<WorkRecordEntity>)
}
