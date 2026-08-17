package com.overtime.worker.domain

import com.overtime.worker.domain.calculator.OvertimeCalculator
import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.CalculationPolicy
import com.overtime.worker.domain.model.CurrencyCode
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeCalculationInput
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OvertimeCalculatorTest {
    private val calculator = OvertimeCalculator()
    private val policy = CalculationPolicy(CurrencyCode.YER, 0, RoundingMode.HALF_UP)
    private val start = LocalDateTime.of(2026, 8, 17, 18, 15)
    private fun hourly(rate: String, end: LocalDateTime = start.plusMinutes(150), multiplier: String = "1.5") = OvertimeCalculationInput(CalculationMethod.HOURLY_RATE, start, end, BigDecimal(multiplier), CalculationMethodInput.HourlyRate(Money(BigDecimal(rate))))

    @Test fun calculates210MinutesAnd35Hours() { val result = calculator.calculate(hourly("500"), policy).getOrThrow(); assertEquals(BigDecimal("3.5"), result.overtimeHours); assertEquals(BigDecimal("1875"), result.overtimePay.amount) }
    @Test fun supportsCrossMidnight() { val result = calculator.calculate(hourly("500", LocalDateTime.of(2026, 8, 18, 2, 0)), policy).getOrThrow(); assertEquals(BigDecimal("4"), result.overtimeHours) }
    @Test fun salaryBasedDerivesHourlyRateAndRoundsFinalPay() { val input = OvertimeCalculationInput(CalculationMethod.SALARY_BASED, start, start.plusMinutes(150), BigDecimal("1.5"), CalculationMethodInput.SalaryBased(Money(BigDecimal("30000")), BigDecimal("30"), BigDecimal("8"))); val result = calculator.calculate(input, policy).getOrThrow(); assertEquals(BigDecimal("125"), result.hourlyRateUsed.amount); assertEquals(BigDecimal("469"), result.overtimePay.amount) }
    @Test fun salaryBasedUsesPolicyDefaults() { val input = OvertimeCalculationInput(CalculationMethod.SALARY_BASED, start, start.plusMinutes(150), BigDecimal("1.5"), CalculationMethodInput.SalaryBased(Money(BigDecimal("30000")))); val result = calculator.calculate(input, policy).getOrThrow(); assertEquals(BigDecimal("125"), result.hourlyRateUsed.amount) }
    @Test fun nonTerminatingSalaryDivisionDoesNotThrow() { val input = OvertimeCalculationInput(CalculationMethod.SALARY_BASED, start, start.plusMinutes(60), BigDecimal("1"), CalculationMethodInput.SalaryBased(Money(BigDecimal("100")), BigDecimal("7"), BigDecimal("8"))); val result = calculator.calculate(input, policy).getOrThrow(); assertTrue(result.hourlyRateUsed.amount > BigDecimal.ZERO) }
    @Test fun oneMinuteIsSupportedWithMathContext() { val result = calculator.calculate(hourly("500", start.plusMinutes(1), "1"), policy).getOrThrow(); assertEquals(BigDecimal.ONE.divide(BigDecimal(60), MathContext.DECIMAL128), result.overtimeHours) }
    @Test fun twoMinutesIsSupported() { val result = calculator.calculate(hourly("500", start.plusMinutes(2), "1"), policy).getOrThrow(); assertEquals(BigDecimal("2").divide(BigDecimal(60), MathContext.DECIMAL128), result.overtimeHours) }
    @Test fun thirtyMinutesEqualsHalfHour() { val result = calculator.calculate(hourly("500", start.plusMinutes(30), "1"), policy).getOrThrow(); assertEquals(BigDecimal("0.5"), result.overtimeHours) }
    @Test fun rejectsZeroDurationAndEndBeforeStart() { assertTrue(calculator.calculate(hourly("500", start), policy).isFailure); assertTrue(calculator.calculate(hourly("500", start.minusMinutes(1)), policy).isFailure) }
    @Test fun rejectsZeroNegativeAndMissingValues() { assertTrue(calculator.calculate(hourly("0"), policy).isFailure); assertTrue(calculator.calculate(hourly("-1"), policy).isFailure) }
    @Test fun supportsLargeFinancialValues() { val result = calculator.calculate(hourly("999999999999999999999999"), policy).getOrThrow(); assertTrue(result.overtimePay.amount > BigDecimal.ZERO) }
    @Test fun explanationContainsActualOperations() { val result = calculator.calculate(hourly("500"), policy).getOrThrow(); val text = result.explanation.steps.joinToString("|") { "${it.label}:${it.expression}:${it.result}" }; assertTrue(text.contains("500")); assertTrue(text.contains("150 دقيقة")); assertTrue(text.contains("1.5")); assertTrue(text.contains("1875")) }
    @Test fun repeatedCalculationIsDeterministic() { val input = hourly("500"); assertEquals(calculator.calculate(input, policy).getOrThrow(), calculator.calculate(input, policy).getOrThrow()) }
    @Test fun salaryBasedRejectsZeroDivisors() { val input = OvertimeCalculationInput(CalculationMethod.SALARY_BASED, start, start.plusHours(1), BigDecimal("1.5"), CalculationMethodInput.SalaryBased(Money(BigDecimal("30000")), BigDecimal.ZERO, BigDecimal("8"))); assertTrue(calculator.calculate(input, policy).isFailure) }
}
