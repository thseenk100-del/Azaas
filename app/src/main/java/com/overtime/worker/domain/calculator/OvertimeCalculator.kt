package com.overtime.worker.domain.calculator

import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.CalculationPolicy
import com.overtime.worker.domain.model.ExplanationModel
import com.overtime.worker.domain.model.ExplanationStep
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeCalculationInput
import com.overtime.worker.domain.model.OvertimeCalculationResult
import com.overtime.worker.domain.model.toDecimalHours
import java.math.BigDecimal
import java.time.Duration

class OvertimeCalculator(private val validation: OvertimeValidation = OvertimeValidation()) {
    fun calculate(input: OvertimeCalculationInput, policy: CalculationPolicy = CalculationPolicy()): Result<OvertimeCalculationResult> {
        val errors = validation.validate(input, policy.defaultWorkingDaysPerMonth, policy.defaultWorkingHoursPerDay)
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("؛ ")))

        val duration = Duration.between(input.overtimeStart, input.overtimeEnd)
        val minutes = duration.toMinutes()
        if (minutes <= 0) return Result.failure(IllegalArgumentException("مدة الإضافي يجب أن تكون أكبر من صفر"))
        val overtimeHours = duration.toDecimalHours(policy.intermediateMathContext)
        val multiplier = input.overtimeMultiplier
        val steps = mutableListOf<ExplanationStep>()

        val hourlyRate: Money = when (val methodInput = input.methodInput) {
            is CalculationMethodInput.HourlyRate -> {
                steps += ExplanationStep("أجر الساعة", "القيمة المدخلة مباشرة", methodInput.hourlyRate.amount.stripTrailingZeros().toPlainString())
                methodInput.hourlyRate
            }
            is CalculationMethodInput.SalaryBased -> {
                val days = methodInput.workingDaysPerMonth ?: policy.defaultWorkingDaysPerMonth
                val hours = methodInput.workingHoursPerDay ?: policy.defaultWorkingHoursPerDay
                val monthlyHours = days.multiply(hours, policy.intermediateMathContext)
                val derivedRate = methodInput.monthlySalary.amount.divide(monthlyHours, policy.intermediateMathContext)
                steps += ExplanationStep("إجمالي الساعات الشهرية", "${days.stripTrailingZeros()} × ${hours.stripTrailingZeros()}", monthlyHours.stripTrailingZeros().toPlainString())
                steps += ExplanationStep("أجر الساعة", "${methodInput.monthlySalary.amount.stripTrailingZeros().toPlainString()} ÷ ${monthlyHours.stripTrailingZeros().toPlainString()}", derivedRate.stripTrailingZeros().toPlainString())
                Money(derivedRate, methodInput.monthlySalary.currency)
            }
        }

        val finalPay = hourlyRate.amount.multiply(overtimeHours, policy.intermediateMathContext).multiply(multiplier, policy.intermediateMathContext)
        steps += ExplanationStep("مدة الإضافي", "$minutes دقيقة ÷ 60", overtimeHours.stripTrailingZeros().toPlainString())
        steps += ExplanationStep("معامل الإضافي", "${multiplier.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()}%", multiplier.stripTrailingZeros().toPlainString())
        steps += ExplanationStep("قيمة الإضافي", "${hourlyRate.amount.stripTrailingZeros().toPlainString()} × ${overtimeHours.stripTrailingZeros().toPlainString()} × ${multiplier.stripTrailingZeros().toPlainString()}", finalPay.stripTrailingZeros().toPlainString())

        return Result.success(OvertimeCalculationResult(hourlyRate, input.overtimeStart, input.overtimeEnd, overtimeHours, multiplier, policy.roundFinal(Money(finalPay, hourlyRate.currency)), ExplanationModel(steps)))
    }
}
