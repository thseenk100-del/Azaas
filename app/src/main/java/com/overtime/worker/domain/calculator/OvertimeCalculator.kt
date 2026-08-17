package com.overtime.worker.domain.calculator

import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import kotlin.math.round

class OvertimeCalculator {
    fun calculate(input: OvertimeInput): Result<OvertimeResult> {
        if (input.hourlyRate < 0 || input.regularHours < 0 || input.overtimeHours < 0 ||
            input.overtimeMultiplier < 0 || input.allowance < 0 || input.deductions < 0
        ) return Result.failure(IllegalArgumentException("لا يمكن استخدام قيم سالبة"))

        val regularPay = input.hourlyRate * input.regularHours
        val overtimePay = input.hourlyRate * input.overtimeHours * input.overtimeMultiplier
        val grossPay = regularPay + overtimePay + input.allowance
        val netPay = (grossPay - input.deductions).coerceAtLeast(0.0)

        return Result.success(
            OvertimeResult(
                regularPay = regularPay.roundMoney(),
                overtimePay = overtimePay.roundMoney(),
                grossPay = grossPay.roundMoney(),
                netPay = netPay.roundMoney()
            )
        )
    }

    private fun Double.roundMoney(): Double = round(this * 100) / 100
}
