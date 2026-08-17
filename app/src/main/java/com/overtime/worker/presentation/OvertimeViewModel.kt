package com.overtime.worker.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overtime.worker.data.OvertimeRepository
import com.overtime.worker.data.local.OvertimeDatabase
import com.overtime.worker.data.preferences.SettingsRepository
import com.overtime.worker.domain.calculator.OvertimeCalculator
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.OvertimeCalculationInput
import com.overtime.worker.domain.model.OvertimeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OvertimeRepository(OvertimeDatabase.getInstance(application), SettingsRepository(application))
    private val calculator = OvertimeCalculator()
    private val query = MutableStateFlow("")
    private val month = MutableStateFlow(LocalDate.now().toString().take(7))

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val history: StateFlow<List<OvertimeRecord>> = repository.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val filteredHistory: StateFlow<List<OvertimeRecord>> = combine(history, query, month) { records, text, selectedMonth -> records.filter { it.date.startsWith(selectedMonth) && (text.isBlank() || it.notes.contains(text, true) || it.date.contains(text)) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastResult = MutableStateFlow<com.overtime.worker.domain.model.OvertimeCalculationResult?>(null)
    val lastResult: StateFlow<com.overtime.worker.domain.model.OvertimeCalculationResult?> = _lastResult.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setQuery(value: String) { query.value = value }
    fun setMonth(value: String) { month.value = value }

    fun calculate(input: OvertimeCalculationInput, onSaved: (OvertimeRecord) -> Unit = {}) {
        calculator.calculate(input).onSuccess { result ->
            _error.value = null; _lastResult.value = result
            viewModelScope.launch {
                val record = OvertimeRecord(
                    calculationMethod = input.calculationMethod,
                    date = input.overtimeStart.toLocalDate().toString(),
                    overtimeStart = result.overtimeStart,
                    overtimeEnd = result.overtimeEnd,
                    overtimeHours = result.overtimeHours,
                    hourlyRate = if (input.methodInput is com.overtime.worker.domain.model.CalculationMethodInput.HourlyRate) input.methodInput.hourlyRate else null,
                    monthlySalary = if (input.methodInput is com.overtime.worker.domain.model.CalculationMethodInput.SalaryBased) input.methodInput.monthlySalary else null,
                    workingDaysPerMonth = if (input.methodInput is com.overtime.worker.domain.model.CalculationMethodInput.SalaryBased) input.methodInput.workingDaysPerMonth else null,
                    workingHoursPerDay = if (input.methodInput is com.overtime.worker.domain.model.CalculationMethodInput.SalaryBased) input.methodInput.workingHoursPerDay else null,
                    overtimeMultiplier = result.overtimeMultiplier,
                    overtimePay = result.overtimePay,
                    currency = input.currency
                )
                repository.save(record); onSaved(record); _message.value = "تم حفظ العملية"
            }
        }.onFailure { _error.value = it.message ?: "تحقق من البيانات المدخلة" }
    }

    fun delete(record: OvertimeRecord) { viewModelScope.launch { repository.delete(record); _message.value = "تم حذف العملية" } }
    fun clearHistory() { viewModelScope.launch { repository.clear(); _message.value = "تم مسح السجل" } }
    fun saveSettings(settings: AppSettings) { viewModelScope.launch { repository.saveSettings(settings); _message.value = "تم حفظ الإعدادات" } }
    fun clearMessages() { _message.value = null; _error.value = null }
}
