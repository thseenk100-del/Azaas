package com.overtime.worker.domain

import com.overtime.worker.domain.calculator.OvertimeCalculator
import com.overtime.worker.domain.model.OvertimeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OvertimeCalculatorTest {
    private val calculator = OvertimeCalculator()

    @Test fun calculatesRegularAndOvertimePay() {
        val result = calculator.calculate(OvertimeInput(25.0, 8.0, 2.0, 1.5)).getOrThrow()
        assertEquals(200.0, result.regularPay, 0.001)
        assertEquals(75.0, result.overtimePay, 0.001)
        assertEquals(275.0, result.netPay, 0.001)
    }

    @Test fun includesAllowanceAndDeduction() {
        val result = calculator.calculate(OvertimeInput(20.0, 8.0, 1.0, 2.0, 50.0, 10.0)).getOrThrow()
        assertEquals(360.0, result.grossPay, 0.001)
        assertEquals(350.0, result.netPay, 0.001)
    }

    @Test fun rejectsNegativeValues() {
        assertTrue(calculator.calculate(OvertimeInput(-1.0, 8.0, 0.0, 1.5)).isFailure)
    }

    @Test fun clampsNetPayAtZero() {
        val result = calculator.calculate(OvertimeInput(10.0, 1.0, 0.0, 1.5, 0.0, 100.0)).getOrThrow()
        assertEquals(0.0, result.netPay, 0.001)
    }
}
