package com.overtime.worker.domain.model

import java.time.LocalDate
import java.util.UUID

data class OvertimeInput(
    val hourlyRate: Double,
    val regularHours: Double,
    val overtimeHours: Double,
    val overtimeMultiplier: Double,
    val allowance: Double = 0.0,
    val deductions: Double = 0.0
)

data class OvertimeResult(
    val regularPay: Double,
    val overtimePay: Double,
    val grossPay: Double,
    val netPay: Double
)

data class CalculationRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val input: OvertimeInput,
    val result: OvertimeResult
)

data class AppSettings(
    val defaultMultiplier: Double = 1.5,
    val currency: String = "ر.س"
)
