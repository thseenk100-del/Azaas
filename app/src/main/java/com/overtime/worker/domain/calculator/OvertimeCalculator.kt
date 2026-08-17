package com.overtime.worker.domain.calculator

import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import java.math.BigDecimal
import java.math.RoundingMode

class OvertimeCalculator {
    fun calculate(input: OvertimeInput): Result<OvertimeResult> {
        val values = listOf(input.hourlyRate, input.regularHours, input.overtimeHours, input.overtimeMultiplier, input.allowance, input.deductions)
        if (values.any { !it.isFinite() || it < 0.0 }) return Result.failure(IllegalArgumentException("أدخل قيمًا موجبة وصحيحة"))

        val rate = input.hourlyRate.bd()
        val regularPay = rate.multiply(input.regularHours.bd())
        val overtimePay = rate.multiply(input.overtimeHours.bd()).multiply(input.overtimeMultiplier.bd())
        val grossPay = regularPay.add(overtimePay).add(input.allowance.bd())
        val netPay = grossPay.subtract(input.deductions.bd()).max(BigDecimal.ZERO)

        return Result.success(
            OvertimeResult(
                regularPay = regularPay.money(),
                overtimePay = overtimePay.money(),
                grossPay = grossPay.money(),
                netPay = netPay.money()
            )
        )
    }

    private fun Double.bd() = BigDecimal.valueOf(this)
    private fun BigDecimal.money(): Double = setScale(2, RoundingMode.HALF_UP).toDouble()
}
