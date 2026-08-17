package com.overtime.worker.backup

import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CurrencyCode
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeRecord
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime

object BackupManager {
    fun export(records: List<OvertimeRecord>): String {
        val root = JSONObject().put("format", "how-counted-overtime-backup-v2")
        val array = JSONArray()
        records.forEach { record -> array.put(JSONObject().apply {
            put("id", record.id); put("calculationMethod", record.calculationMethod.name); put("date", record.date)
            put("overtimeStart", record.overtimeStart.toString()); put("overtimeEnd", record.overtimeEnd.toString())
            put("overtimeHours", record.overtimeHours.toPlainString()); put("hourlyRate", record.hourlyRate?.amount?.toPlainString())
            put("monthlySalary", record.monthlySalary?.amount?.toPlainString()); put("workingDaysPerMonth", record.workingDaysPerMonth?.toPlainString())
            put("workingHoursPerDay", record.workingHoursPerDay?.toPlainString()); put("multiplier", record.overtimeMultiplier.toPlainString())
            put("overtimePay", record.overtimePay.amount.toPlainString()); put("currency", record.currency.value); put("notes", record.notes)
            put("createdAt", record.createdAt); put("updatedAt", record.updatedAt)
        }) }
        return root.put("records", array).toString(2)
    }

    fun restore(json: String): Result<List<OvertimeRecord>> = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "how-counted-overtime-backup-v2") { "ملف النسخ الاحتياطي غير مدعوم" }
        val array = root.getJSONArray("records")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i); val currency = CurrencyCode(item.optString("currency", "YER"))
                add(OvertimeRecord(
                    id = item.getString("id"), calculationMethod = CalculationMethod.valueOf(item.getString("calculationMethod")), date = item.getString("date"),
                    overtimeStart = LocalDateTime.parse(item.getString("overtimeStart")), overtimeEnd = LocalDateTime.parse(item.getString("overtimeEnd")),
                    overtimeHours = BigDecimal(item.getString("overtimeHours")), hourlyRate = item.optString("hourlyRate", "").takeIf(String::isNotBlank)?.let { Money(BigDecimal(it), currency) },
                    monthlySalary = item.optString("monthlySalary", "").takeIf(String::isNotBlank)?.let { Money(BigDecimal(it), currency) },
                    workingDaysPerMonth = item.optString("workingDaysPerMonth", "").takeIf(String::isNotBlank)?.let(::BigDecimal),
                    workingHoursPerDay = item.optString("workingHoursPerDay", "").takeIf(String::isNotBlank)?.let(::BigDecimal),
                    overtimeMultiplier = BigDecimal(item.getString("multiplier")), overtimePay = Money(BigDecimal(item.getString("overtimePay")), currency),
                    currency = currency, notes = item.optString("notes"), createdAt = item.optLong("createdAt"), updatedAt = item.optLong("updatedAt")
                ))
            }
        }
    }
}
