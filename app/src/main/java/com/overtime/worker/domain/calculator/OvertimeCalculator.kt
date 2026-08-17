package com.overtime.worker.domain.calculator

import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.CalculationPolicy
import com.overtime.worker.domain.model.ExplanationModel
import com.overtime.worker.domain.model.ExplanationStep
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeCalculationInput
import com.overtime.worker.domain.model.OvertimeCalculationResult
import com.overtime.worker.domain.model.toExactHours
import java.math.BigDecimal
import java.time.Duration

class OvertimeCalculator(private val validation: OvertimeValidation = OvertimeValidation()) {
    fun calculate(input: OvertimeCalculationInput, policy: CalculationPolicy = CalculationPolicy()): Result<OvertimeCalculationResult> {
        val errors = validation.validate(input)
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("؛ ")))

        val duration = Duration.between(input.overtimeStart, input.overtimeEnd)
        val minutes = duration.toMinutes()
        if (minutes <= 0) return Result.failure(IllegalArgumentException("مدة الإضافي يجب أن تكون أكبر من صفر"))
        val overtimeHours = duration.toExactHours()
        val multiplier = input.overtimeMultiplier
        val steps = mutableListOf<ExplanationStep>()

        val hourlyRate: Money = when (val methodInput = input.methodInput) {
            is CalculationMethodInput.HourlyRate -> {
                steps += ExplanationStep("أجر الساعة", "القيمة المدخلة مباشرة", methodInput.hourlyRate.amount.stripTrailingZeros().toPlainString())
                methodInput.hourlyRate
            }
            is CalculationMethodInput.SalaryBased -> {
                val monthlyHours = methodInput.workingDaysPerMonth.multiply(methodInput.workingHoursPerDay)
                val derivedRate = methodInput.monthlySalary.amount.divide(monthlyHours)
                steps += ExplanationStep("إجمالي الساعات الشهرية", "${methodInput.workingDaysPerMonth} × ${methodInput.workingHoursPerDay}", monthlyHours.stripTrailingZeros().toPlainString())
                steps += ExplanationStep("أجر الساعة", "${methodInput.monthlySalary.amount.stripTrailingZeros().toPlainString()} ÷ ${monthlyHours.stripTrailingZeros().toPlainString()}", derivedRate.stripTrailingZeros().toPlainString())
                Money(derivedRate, methodInput.monthlySalary.currency)
            }
        }

        val finalPay = hourlyRate.amount.multiply(overtimeHours).multiply(multiplier)
        steps += ExplanationStep("مدة الإضافي", "$minutes دقيقة ÷ 60", overtimeHours.stripTrailingZeros().toPlainString())
        steps += ExplanationStep("معامل الإضافي", "${multiplier.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()}%", multiplier.stripTrailingZeros().toPlainString())
        steps += ExplanationStep("قيمة الإضافي", "${hourlyRate.amount.stripTrailingZeros().toPlainString()} × ${overtimeHours.stripTrailingZeros().toPlainString()} × ${multiplier.stripTrailingZeros().toPlainString()}", finalPay.stripTrailingZeros().toPlainString())

        return Result.success(
            OvertimeCalculationResult(
                hourlyRateUsed = hourlyRate,
                overtimeStart = input.overtimeStart,
                overtimeEnd = input.overtimeEnd,
                overtimeHours = overtimeHours,
                overtimeMultiplier = multiplier,
                overtimePay = policy.roundFinal(Money(finalPay, hourlyRate.currency)),
                explanation = ExplanationModel(steps)
            )
        )
    }
}
