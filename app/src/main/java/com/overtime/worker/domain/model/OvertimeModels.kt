package com.overtime.worker.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

enum class CalculationMethod { HOURLY_RATE, SALARY_BASED }

data class CurrencyCode(val value: String) {
    init { require(value.matches(Regex("[A-Z]{3}"))) { "رمز العملة يجب أن يكون من ثلاثة أحرف لاتينية كبيرة" } }
    companion object { val YER = CurrencyCode("YER") }
}

data class Money(val amount: BigDecimal, val currency: CurrencyCode = CurrencyCode.YER) {
    operator fun plus(other: Money): Money { require(currency == other.currency); return Money(amount + other.amount, currency) }
    operator fun times(factor: BigDecimal): Money = Money(amount * factor, currency)
}

data class CalculationPolicy(
    val currency: CurrencyCode = CurrencyCode.YER,
    val finalScale: Int = 0,
    val roundingMode: RoundingMode = RoundingMode.HALF_UP,
    val defaultWorkingDaysPerMonth: BigDecimal = BigDecimal("30"),
    val defaultWorkingHoursPerDay: BigDecimal = BigDecimal("8")
) {
    fun roundFinal(money: Money): Money = Money(money.amount.setScale(finalScale, roundingMode), money.currency)
}

sealed interface CalculationMethodInput {
    data class HourlyRate(val hourlyRate: Money) : CalculationMethodInput
    data class SalaryBased(
        val monthlySalary: Money,
        val workingDaysPerMonth: BigDecimal,
        val workingHoursPerDay: BigDecimal
    ) : CalculationMethodInput
}

data class OvertimeCalculationInput(
    val calculationMethod: CalculationMethod,
    val overtimeStart: LocalDateTime,
    val overtimeEnd: LocalDateTime,
    val overtimeMultiplier: BigDecimal,
    val methodInput: CalculationMethodInput,
    val currency: CurrencyCode = CurrencyCode.YER
)

data class ExplanationStep(
    val label: String,
    val expression: String,
    val result: String
)

data class ExplanationModel(val steps: List<ExplanationStep>)

data class OvertimeCalculationResult(
    val hourlyRateUsed: Money,
    val overtimeStart: LocalDateTime,
    val overtimeEnd: LocalDateTime,
    val overtimeHours: BigDecimal,
    val overtimeMultiplier: BigDecimal,
    val overtimePay: Money,
    val explanation: ExplanationModel
)

data class OvertimeRecord(
    val id: String = UUID.randomUUID().toString(),
    val calculationMethod: CalculationMethod,
    val date: String,
    val overtimeStart: LocalDateTime,
    val overtimeEnd: LocalDateTime,
    val overtimeHours: BigDecimal,
    val hourlyRate: Money? = null,
    val monthlySalary: Money? = null,
    val workingDaysPerMonth: BigDecimal? = null,
    val workingHoursPerDay: BigDecimal? = null,
    val overtimeMultiplier: BigDecimal,
    val overtimePay: Money,
    val currency: CurrencyCode = CurrencyCode.YER,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

internal fun Duration.toExactHours(): BigDecimal = BigDecimal.valueOf(toMinutes()).divide(BigDecimal(60))


data class AppSettings(
    val defaultMultiplier: BigDecimal = BigDecimal("1.5"),
    val currency: CurrencyCode = CurrencyCode.YER,
    val workingDaysPerMonth: BigDecimal = BigDecimal("30"),
    val workingHoursPerDay: BigDecimal = BigDecimal("8"),
    val defaultMonthlySalary: Money = Money(BigDecimal.ZERO, CurrencyCode.YER),
    val employeeName: String = ""
)
