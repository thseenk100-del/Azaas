package com.overtime.worker.data

import android.content.Context
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CalculationRecord
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import org.json.JSONArray
import org.json.JSONObject

class LocalOvertimeRepository(context: Context) {
    private val preferences = context.getSharedPreferences("overtime_worker", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings = AppSettings(
        defaultMultiplier = preferences.getString(KEY_MULTIPLIER, "1.5")?.toDoubleOrNull() ?: 1.5,
        currency = preferences.getString(KEY_CURRENCY, "ر.س") ?: "ر.س"
    )

    fun saveSettings(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_MULTIPLIER, settings.defaultMultiplier.toString())
            .putString(KEY_CURRENCY, settings.currency)
            .apply()
    }

    fun loadHistory(): List<CalculationRecord> = runCatching {
        val array = JSONArray(preferences.getString(KEY_HISTORY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    CalculationRecord(
                        id = item.getString("id"),
                        date = item.getString("date"),
                        input = OvertimeInput(
                            hourlyRate = item.getDouble("hourlyRate"),
                            regularHours = item.getDouble("regularHours"),
                            overtimeHours = item.getDouble("overtimeHours"),
                            overtimeMultiplier = item.getDouble("multiplier"),
                            allowance = item.getDouble("allowance"),
                            deductions = item.getDouble("deductions")
                        ),
                        result = OvertimeResult(
                            regularPay = item.getDouble("regularPay"),
                            overtimePay = item.getDouble("overtimePay"),
                            grossPay = item.getDouble("grossPay"),
                            netPay = item.getDouble("netPay")
                        )
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun saveRecord(record: CalculationRecord) {
        val records = loadHistory().toMutableList()
        records.removeAll { it.id == record.id }
        records.add(0, record)
        val array = JSONArray()
        records.take(MAX_HISTORY).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("date", item.date)
                put("hourlyRate", item.input.hourlyRate)
                put("regularHours", item.input.regularHours)
                put("overtimeHours", item.input.overtimeHours)
                put("multiplier", item.input.overtimeMultiplier)
                put("allowance", item.input.allowance)
                put("deductions", item.input.deductions)
                put("regularPay", item.result.regularPay)
                put("overtimePay", item.result.overtimePay)
                put("grossPay", item.result.grossPay)
                put("netPay", item.result.netPay)
            })
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clearHistory() { preferences.edit().remove(KEY_HISTORY).apply() }

    private companion object {
        const val KEY_HISTORY = "history"
        const val KEY_MULTIPLIER = "multiplier"
        const val KEY_CURRENCY = "currency"
        const val MAX_HISTORY = 100
    }
}
