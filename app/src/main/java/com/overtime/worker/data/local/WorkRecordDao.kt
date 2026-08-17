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
    @Query("SELECT * FROM overtime_records ORDER BY date DESC, overtimeStart DESC")
    fun observeAll(): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM overtime_records WHERE date LIKE :month || '%' ORDER BY date DESC, overtimeStart DESC")
    fun observeMonth(month: String): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM overtime_records WHERE date LIKE :month || '%' AND (notes LIKE '%' || :query || '%' OR date LIKE '%' || :query || '%') ORDER BY date DESC, overtimeStart DESC")
    fun searchMonth(month: String, query: String): Flow<List<WorkRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: WorkRecordEntity)

    @Update
    suspend fun update(record: WorkRecordEntity)

    @Delete
    suspend fun delete(record: WorkRecordEntity)

    @Query("DELETE FROM overtime_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM overtime_records ORDER BY date DESC, overtimeStart DESC")
    suspend fun snapshot(): List<WorkRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<WorkRecordEntity>)
}
