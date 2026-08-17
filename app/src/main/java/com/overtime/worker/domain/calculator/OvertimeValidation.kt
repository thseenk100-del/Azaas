package com.overtime.worker.domain.calculator

import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.OvertimeCalculationInput
import java.math.BigDecimal

class OvertimeValidation {
    fun validate(input: OvertimeCalculationInput, defaultDays: BigDecimal = BigDecimal("30"), defaultHours: BigDecimal = BigDecimal("8")): List<String> = buildList {
        if (!input.overtimeEnd.isAfter(input.overtimeStart)) add("يجب أن تكون نهاية الإضافي بعد بدايته")
        if (input.overtimeMultiplier <= BigDecimal.ZERO) add("معامل الإضافي يجب أن يكون أكبر من صفر")
        if (input.currency != input.methodInput.currency()) add("يجب توحيد العملة داخل العملية")
        when (input.calculationMethod) {
            CalculationMethod.HOURLY_RATE -> {
                val hourly = (input.methodInput as? CalculationMethodInput.HourlyRate)?.hourlyRate
                if (hourly == null) add("أدخل أجر الساعة")
                else if (hourly.amount <= BigDecimal.ZERO) add("أجر الساعة يجب أن يكون أكبر من صفر")
            }
            CalculationMethod.SALARY_BASED -> {
                val salary = input.methodInput as? CalculationMethodInput.SalaryBased
                if (salary == null) add("أدخل بيانات الراتب")
                else {
                    if (salary.monthlySalary.amount <= BigDecimal.ZERO) add("الراتب الشهري يجب أن يكون أكبر من صفر")
                    if ((salary.workingDaysPerMonth ?: defaultDays) <= BigDecimal.ZERO) add("أيام العمل الشهرية يجب أن تكون أكبر من صفر")
                    if ((salary.workingHoursPerDay ?: defaultHours) <= BigDecimal.ZERO) add("ساعات العمل اليومية يجب أن تكون أكبر من صفر")
                }
            }
        }
    }

    private fun CalculationMethodInput.currency() = when (this) {
        is CalculationMethodInput.HourlyRate -> hourlyRate.currency
        is CalculationMethodInput.SalaryBased -> monthlySalary.currency
    }
}
