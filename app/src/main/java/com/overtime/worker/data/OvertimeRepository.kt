package com.overtime.worker.data

import com.overtime.worker.data.local.OvertimeDatabase
import com.overtime.worker.data.local.WorkRecordEntity
import com.overtime.worker.data.preferences.SettingsRepository
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CalculationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OvertimeRepository(
    private val database: OvertimeDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val dao = database.workRecordDao()

    val settings: Flow<AppSettings> = settingsRepository.settings
    val records: Flow<List<CalculationRecord>> = dao.observeAll().map { list -> list.map(WorkRecordEntity::toDomain) }

    fun observeMonth(month: String, query: String): Flow<List<CalculationRecord>> =
        dao.searchMonth(month, query).map { list -> list.map(WorkRecordEntity::toDomain) }

    suspend fun saveSettings(settings: AppSettings) = settingsRepository.save(settings)
    suspend fun save(record: CalculationRecord) = dao.upsert(WorkRecordEntity.fromDomain(record))
    suspend fun delete(record: CalculationRecord) = dao.delete(WorkRecordEntity.fromDomain(record))
    suspend fun clear() = dao.deleteAll()
    suspend fun snapshot(): List<CalculationRecord> = dao.snapshot().map(WorkRecordEntity::toDomain)
    suspend fun restore(records: List<CalculationRecord>) = dao.insertAll(records.map(WorkRecordEntity::fromDomain))
}
