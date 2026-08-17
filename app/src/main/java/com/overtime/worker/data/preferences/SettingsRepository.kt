package com.overtime.worker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CurrencyCode
import com.overtime.worker.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

private val Context.settingsDataStore by preferencesDataStore(name = "overtime_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val multiplier = stringPreferencesKey("default_multiplier")
        val currency = stringPreferencesKey("currency")
        val workingDays = stringPreferencesKey("working_days_per_month")
        val workingHours = stringPreferencesKey("working_hours_per_day")
        val monthlySalary = stringPreferencesKey("default_monthly_salary")
        val employeeName = stringPreferencesKey("employee_name")
    }

    private fun String?.decimalOr(default: String) = runCatching { BigDecimal(this ?: default) }.getOrDefault(BigDecimal(default))

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        val currency = runCatching { CurrencyCode(values[Keys.currency] ?: "YER") }.getOrDefault(CurrencyCode.YER)
        AppSettings(
            defaultMultiplier = values[Keys.multiplier].decimalOr("1.5"),
            currency = currency,
            workingDaysPerMonth = values[Keys.workingDays].decimalOr("30"),
            workingHoursPerDay = values[Keys.workingHours].decimalOr("8"),
            defaultMonthlySalary = Money(values[Keys.monthlySalary].decimalOr("0"), currency),
            employeeName = values[Keys.employeeName] ?: ""
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { values ->
            values[Keys.multiplier] = settings.defaultMultiplier.toPlainString()
            values[Keys.currency] = settings.currency.value
            values[Keys.workingDays] = settings.workingDaysPerMonth.toPlainString()
            values[Keys.workingHours] = settings.workingHoursPerDay.toPlainString()
            values[Keys.monthlySalary] = settings.defaultMonthlySalary.amount.toPlainString()
            values[Keys.employeeName] = settings.employeeName
        }
    }
}
