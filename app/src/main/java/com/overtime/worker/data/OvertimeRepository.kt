package com.overtime.worker.data

import com.overtime.worker.data.local.OvertimeDatabase
import com.overtime.worker.data.local.WorkRecordEntity
import com.overtime.worker.data.preferences.SettingsRepository
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.OvertimeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OvertimeRepository(
    private val database: OvertimeDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val dao = database.workRecordDao()
    val settings: Flow<AppSettings> = settingsRepository.settings
    val records: Flow<List<OvertimeRecord>> = dao.observeAll().map { it.map(WorkRecordEntity::toDomain) }
    fun observeMonth(month: String, query: String): Flow<List<OvertimeRecord>> = dao.searchMonth(month, query).map { it.map(WorkRecordEntity::toDomain) }
    suspend fun save(record: OvertimeRecord) = dao.upsert(WorkRecordEntity.fromDomain(record))
    suspend fun update(record: OvertimeRecord) = dao.update(WorkRecordEntity.fromDomain(record))
    suspend fun delete(record: OvertimeRecord) = dao.delete(WorkRecordEntity.fromDomain(record))
    suspend fun clear() = dao.deleteAll()
    suspend fun snapshot(): List<OvertimeRecord> = dao.snapshot().map(WorkRecordEntity::toDomain)
    suspend fun restore(records: List<OvertimeRecord>) = dao.insertAll(records.map(WorkRecordEntity::fromDomain))
    suspend fun saveSettings(settings: AppSettings) = settingsRepository.save(settings)
}
