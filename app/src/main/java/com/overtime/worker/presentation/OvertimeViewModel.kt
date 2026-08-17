package com.overtime.worker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.overtime.worker.data.LocalOvertimeRepository
import com.overtime.worker.domain.calculator.OvertimeCalculator
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CalculationRecord
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalOvertimeRepository(application)
    private val calculator = OvertimeCalculator()

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(repository.loadHistory())
    val history: StateFlow<List<CalculationRecord>> = _history.asStateFlow()

    private val _lastResult = MutableStateFlow<OvertimeResult?>(null)
    val lastResult: StateFlow<OvertimeResult?> = _lastResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun calculate(input: OvertimeInput) {
        calculator.calculate(input).onSuccess { result ->
            _error.value = null
            _lastResult.value = result
            repository.saveRecord(CalculationRecord(input = input, result = result))
            _history.value = repository.loadHistory()
        }.onFailure { _error.value = it.message ?: "تحقق من البيانات المدخلة" }
    }

    fun clearResult() { _lastResult.value = null; _error.value = null }

    fun updateSettings(multiplier: Double, currency: String) {
        val updated = AppSettings(multiplier.coerceAtLeast(0.0), currency.ifBlank { "ر.س" })
        repository.saveSettings(updated)
        _settings.value = updated
    }

    fun clearHistory() {
        repository.clearHistory()
        _history.value = emptyList()
    }
}
