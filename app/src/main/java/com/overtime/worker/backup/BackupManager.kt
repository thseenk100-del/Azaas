package com.overtime.worker.backup

import com.overtime.worker.domain.model.CalculationRecord
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    fun export(records: List<CalculationRecord>): String {
        val root = JSONObject().put("format", "how-counted-backup-v1")
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().apply {
                put("id", record.id); put("date", record.date); put("note", record.note)
                put("hourlyRate", record.input.hourlyRate); put("regularHours", record.input.regularHours)
                put("overtimeHours", record.input.overtimeHours); put("multiplier", record.input.overtimeMultiplier)
                put("allowance", record.input.allowance); put("deductions", record.input.deductions)
                put("regularPay", record.result.regularPay); put("overtimePay", record.result.overtimePay)
                put("grossPay", record.result.grossPay); put("netPay", record.result.netPay)
            })
        }
        return root.put("records", array).toString(2)
    }

    fun restore(json: String): Result<List<CalculationRecord>> = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "how-counted-backup-v1") { "ملف النسخ الاحتياطي غير مدعوم" }
        val array = root.getJSONArray("records")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(CalculationRecord(
                    id = item.getString("id"), date = item.getString("date"), note = item.optString("note"),
                    input = OvertimeInput(item.getDouble("hourlyRate"), item.getDouble("regularHours"), item.getDouble("overtimeHours"), item.getDouble("multiplier"), item.optDouble("allowance"), item.optDouble("deductions")),
                    result = OvertimeResult(item.getDouble("regularPay"), item.getDouble("overtimePay"), item.getDouble("grossPay"), item.getDouble("netPay"))
                ))
            }
        }
    }
}
