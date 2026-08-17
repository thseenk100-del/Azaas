package com.overtime.worker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.CurrencyCode
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeRecord
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(tableName = "overtime_records")
data class WorkRecordEntity(
    @PrimaryKey val id: String,
    val calculationMethod: String,
    val date: String,
    val overtimeStart: String,
    val overtimeEnd: String,
    val overtimeHours: String,
    val hourlyRateAmount: String?,
    val monthlySalaryAmount: String?,
    val workingDaysPerMonth: String?,
    val workingHoursPerDay: String?,
    val overtimeMultiplier: String,
    val overtimePayAmount: String,
    val currencyCode: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): OvertimeRecord {
        val currency = CurrencyCode(currencyCode)
        return OvertimeRecord(
            id = id,
            calculationMethod = CalculationMethod.valueOf(calculationMethod),
            date = date,
            overtimeStart = LocalDateTime.parse(overtimeStart),
            overtimeEnd = LocalDateTime.parse(overtimeEnd),
            overtimeHours = BigDecimal(overtimeHours),
            hourlyRate = hourlyRateAmount?.let { Money(BigDecimal(it), currency) },
            monthlySalary = monthlySalaryAmount?.let { Money(BigDecimal(it), currency) },
            workingDaysPerMonth = workingDaysPerMonth?.let(::BigDecimal),
            workingHoursPerDay = workingHoursPerDay?.let(::BigDecimal),
            overtimeMultiplier = BigDecimal(overtimeMultiplier),
            overtimePay = Money(BigDecimal(overtimePayAmount), currency),
            currency = currency,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(record: OvertimeRecord) = WorkRecordEntity(
            id = record.id,
            calculationMethod = record.calculationMethod.name,
            date = record.date,
            overtimeStart = record.overtimeStart.toString(),
            overtimeEnd = record.overtimeEnd.toString(),
            overtimeHours = record.overtimeHours.toPlainString(),
            hourlyRateAmount = record.hourlyRate?.amount?.toPlainString(),
            monthlySalaryAmount = record.monthlySalary?.amount?.toPlainString(),
            workingDaysPerMonth = record.workingDaysPerMonth?.toPlainString(),
            workingHoursPerDay = record.workingHoursPerDay?.toPlainString(),
            overtimeMultiplier = record.overtimeMultiplier.toPlainString(),
            overtimePayAmount = record.overtimePay.amount.toPlainString(),
            currencyCode = record.currency.value,
            notes = record.notes,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
}
