package com.overtime.worker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.overtime.worker.domain.model.CalculationRecord
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult

@Entity(tableName = "work_records")
data class WorkRecordEntity(
    @PrimaryKey val id: String,
    val workDate: String,
    val note: String,
    val hourlyRateCents: Long,
    val regularHours: Double,
    val overtimeHours: Double,
    val multiplier: Double,
    val allowanceCents: Long,
    val deductionCents: Long,
    val regularPayCents: Long,
    val overtimePayCents: Long,
    val grossPayCents: Long,
    val netPayCents: Long
) {
    fun toDomain() = CalculationRecord(
        id = id,
        date = workDate,
        note = note,
        input = OvertimeInput(
            hourlyRate = hourlyRateCents / 100.0,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            overtimeMultiplier = multiplier,
            allowance = allowanceCents / 100.0,
            deductions = deductionCents / 100.0
        ),
        result = OvertimeResult(
            regularPay = regularPayCents / 100.0,
            overtimePay = overtimePayCents / 100.0,
            grossPay = grossPayCents / 100.0,
            netPay = netPayCents / 100.0
        )
    )

    companion object {
        fun fromDomain(record: CalculationRecord) = WorkRecordEntity(
            id = record.id,
            workDate = record.date,
            note = record.note,
            hourlyRateCents = record.input.hourlyRate.toCents(),
            regularHours = record.input.regularHours,
            overtimeHours = record.input.overtimeHours,
            multiplier = record.input.overtimeMultiplier,
            allowanceCents = record.input.allowance.toCents(),
            deductionCents = record.input.deductions.toCents(),
            regularPayCents = record.result.regularPay.toCents(),
            overtimePayCents = record.result.overtimePay.toCents(),
            grossPayCents = record.result.grossPay.toCents(),
            netPayCents = record.result.netPay.toCents()
        )
    }
}

private fun Double.toCents(): Long = java.math.BigDecimal.valueOf(this).movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
