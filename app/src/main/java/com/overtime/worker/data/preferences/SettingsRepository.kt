package com.overtime.worker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.overtime.worker.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "overtime_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val multiplier = doublePreferencesKey("default_multiplier")
        val currency = stringPreferencesKey("currency")
        val standardHours = doublePreferencesKey("standard_daily_hours")
        val hourlyRate = doublePreferencesKey("default_hourly_rate")
        val employeeName = stringPreferencesKey("employee_name")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        AppSettings(
            defaultMultiplier = values[Keys.multiplier] ?: 1.5,
            currency = values[Keys.currency] ?: "ر.س",
            standardDailyHours = values[Keys.standardHours] ?: 8.0,
            defaultHourlyRate = values[Keys.hourlyRate] ?: 0.0,
            employeeName = values[Keys.employeeName] ?: ""
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { values ->
            values[Keys.multiplier] = settings.defaultMultiplier.coerceAtLeast(0.0)
            values[Keys.currency] = settings.currency.ifBlank { "ر.س" }
            values[Keys.standardHours] = settings.standardDailyHours.coerceAtLeast(0.0)
            values[Keys.hourlyRate] = settings.defaultHourlyRate.coerceAtLeast(0.0)
            values[Keys.employeeName] = settings.employeeName
        }
    }
}
