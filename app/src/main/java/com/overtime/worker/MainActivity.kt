package com.overtime.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CalculationMethod
import com.overtime.worker.domain.model.CalculationMethodInput
import com.overtime.worker.domain.model.CurrencyCode
import com.overtime.worker.domain.model.Money
import com.overtime.worker.domain.model.OvertimeCalculationInput
import com.overtime.worker.domain.model.OvertimeCalculationResult
import com.overtime.worker.domain.model.OvertimeRecord
import com.overtime.worker.report.PdfReportGenerator
import com.overtime.worker.presentation.OvertimeViewModel
import com.overtime.worker.ui.theme.OvertimeWorkerTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { OvertimeWorkerTheme { OvertimeApp() } } }
}

private enum class AppTab(val title: String) { CALCULATOR("الإضافي"), HISTORY("السجل"), REPORTS("التقارير"), SETTINGS("الإعدادات") }
private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
private fun OvertimeApp(viewModel: OvertimeViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CALCULATOR.name) }
    val tab = AppTab.valueOf(selectedTab); val settings by viewModel.settings.collectAsStateWithLifecycle(); val result by viewModel.lastResult.collectAsStateWithLifecycle(); val error by viewModel.error.collectAsStateWithLifecycle(); val message by viewModel.message.collectAsStateWithLifecycle(); val history by viewModel.filteredHistory.collectAsStateWithLifecycle(); val allHistory by viewModel.history.collectAsStateWithLifecycle(); val context = LocalContext.current; val scope = androidx.compose.runtime.rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.use { it.write("[]".toByteArray()) } } }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(topBar = { AppHeader(tab.title) }, bottomBar = { NavigationBar { AppTab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { selectedTab = item.name }, icon = { Text(item.title.take(1)) }, label = { Text(item.title) }) } } }) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
                when (tab) {
                    AppTab.CALCULATOR -> CalculatorScreen(settings, result, error, message) { input -> viewModel.calculate(input) }
                    AppTab.HISTORY -> HistoryScreen(history, viewModel::setQuery, viewModel::setMonth, viewModel::delete, viewModel::clearHistory)
                    AppTab.REPORTS -> ReportsScreen(allHistory, settings, onExport = { exporter.launch("كيف-حسبت-${LocalDate.now()}.json") }, onImport = { importer.launch(arrayOf("application/json", "text/plain")) }, onPdf = { month -> scope.launch { val file = PdfReportGenerator.createMonthlyReport(context, month, allHistory.filter { it.date.startsWith(month) }, settings.currency.value); shareFile(context, file, "تقرير كيف حسبت") } })
                    AppTab.SETTINGS -> SettingsScreen(settings, viewModel::saveSettings)
                }
            }
        }
    }
}

@Composable private fun AppHeader(title: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) { Text("كيف حسبت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(title, color = MaterialTheme.colorScheme.primary) } }

@Composable
private fun CalculatorScreen(settings: AppSettings, result: OvertimeCalculationResult?, error: String?, message: String?, onCalculate: (OvertimeCalculationInput) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }; var start by rememberSaveable { mutableStateOf("18:00") }; var end by rememberSaveable { mutableStateOf("20:00") }; var rate by rememberSaveable { mutableStateOf("") }; var salary by rememberSaveable { mutableStateOf("") }; var days by rememberSaveable { mutableStateOf(settings.workingDaysPerMonth.toPlainString()) }; var dailyHours by rememberSaveable { mutableStateOf(settings.workingHoursPerDay.toPlainString()) }; var multiplier by rememberSaveable { mutableStateOf(settings.defaultMultiplier.toPlainString()) }; var method by rememberSaveable { mutableStateOf(CalculationMethod.HOURLY_RATE.name) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { Spacer(Modifier.height(4.dp)); Text("حساب العمل الإضافي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("أدخل وقت بداية ونهاية الإضافي، ثم اختر طريقة تحديد أجر الساعة.") }
        item { TextField("التاريخ YYYY-MM-DD", date) { date = it } }; item { TextField("بداية الإضافي HH:MM", start) { start = it } }; item { TextField("نهاية الإضافي HH:MM", end) { end = it } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { OutlinedButton(Modifier.weight(1f), onClick = { method = CalculationMethod.HOURLY_RATE.name }) { Text("أجر الساعة") }; OutlinedButton(Modifier.weight(1f), onClick = { method = CalculationMethod.SALARY_BASED.name }) { Text("من الراتب") } } }
        if (method == CalculationMethod.HOURLY_RATE.name) item { NumberField("أجر الساعة", rate) { rate = it } } else { item { NumberField("الراتب الشهري", salary) { salary = it } }; item { NumberField("أيام العمل الشهرية", days) { days = it } }; item { NumberField("ساعات العمل اليومية", dailyHours) { dailyHours = it } } }
        item { NumberField("معامل الإضافي (مثل 1.5 = 150%)", multiplier) { multiplier = it } }
        item { Button(Modifier.fillMaxWidth(), onClick = { parseInput(date, start, end, method, rate, salary, days, dailyHours, multiplier, settings.currency)?.let(onCalculate) }) { Text("احسب الإضافي") } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }; message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }; result?.let { item { ResultCard(it, settings.currency.value) } }; item { Spacer(Modifier.height(20.dp)) }
    }
}

private fun parseInput(date: String, start: String, end: String, method: String, rate: String, salary: String, days: String, hours: String, multiplier: String, currency: CurrencyCode): OvertimeCalculationInput? = runCatching {
    val dateValue = LocalDate.parse(date); val startTime = LocalTime.parse(start); val endTime = LocalTime.parse(end); val startDateTime = LocalDateTime.of(dateValue, startTime); val parsedEndDate = if (endTime.isBefore(startTime)) dateValue.plusDays(1) else dateValue; val endDateTime = LocalDateTime.of(parsedEndDate, endTime); val factor = BigDecimal(multiplier.toEnglishDigits().replace(',', '.')); val methodInput = if (method == CalculationMethod.HOURLY_RATE.name) CalculationMethodInput.HourlyRate(Money(BigDecimal(rate.toEnglishDigits().replace(',', '.')), currency)) else CalculationMethodInput.SalaryBased(Money(BigDecimal(salary.toEnglishDigits().replace(',', '.')), currency), BigDecimal(days.toEnglishDigits().replace(',', '.')), BigDecimal(hours.toEnglishDigits().replace(',', '.'))); OvertimeCalculationInput(CalculationMethod.valueOf(method), startDateTime, endDateTime, factor, methodInput, currency)
}.getOrNull()

@Composable private fun ResultCard(result: OvertimeCalculationResult, currency: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), Arrangement.spacedBy(8.dp)) { Text("كيف حسبت؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("أجر الساعة المستخدم: ${result.hourlyRateUsed.amount} $currency"); Text("المدة: ${result.overtimeHours.stripTrailingZeros().toPlainString()} ساعة"); Text("المعامل: ${result.overtimeMultiplier.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()}%"); Text("قيمة الإضافي: ${result.overtimePay.amount} $currency", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); result.explanation.steps.forEach { Text("${it.label}: ${it.expression} = ${it.result}") } } } }

@Composable private fun HistoryScreen(records: List<OvertimeRecord>, onQuery: (String) -> Unit, onMonth: (String) -> Unit, onDelete: (OvertimeRecord) -> Unit, onClear: () -> Unit) { var query by rememberSaveable { mutableStateOf("") }; var month by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }; Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Text("سجل الإضافي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("بحث بالملاحظة أو التاريخ", query) { query = it; onQuery(it) }; TextField("الشهر YYYY-MM", month) { month = it; onMonth(it) }; Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween) { Text("${records.size} عملية"); OutlinedButton(onClick = onClear) { Text("مسح الكل") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(records, key = { it.id }) { record -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), Arrangement.spacedBy(5.dp)) { Text(record.date); Text("${record.overtimeHours.stripTrailingZeros().toPlainString()} ساعة × ${record.overtimeMultiplier.stripTrailingZeros().toPlainString()} = ${record.overtimePay.amount}"); if (record.notes.isNotBlank()) Text(record.notes); OutlinedButton(onClick = { onDelete(record) }) { Text("حذف") } } } } } } }

@Composable private fun ReportsScreen(records: List<OvertimeRecord>, settings: AppSettings, onExport: () -> Unit, onImport: () -> Unit, onPdf: (String) -> Unit) { var month by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }; val monthly = records.filter { it.date.startsWith(month) }; val hours = monthly.sumOf { it.overtimeHours.toDouble() }; val pay = monthly.sumOf { it.overtimePay.amount.toDouble() }; Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) { Text("تقارير الإضافي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("الشهر YYYY-MM", month) { month = it }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(7.dp)) { Text("ملخص $month", fontWeight = FontWeight.Bold); Text("عدد العمليات: ${monthly.size}"); Text("إجمالي ساعات الإضافي: $hours"); Text("إجمالي قيمة الإضافي: $pay ${settings.currency.value}") } }; Button(Modifier.fillMaxWidth(), onClick = { onPdf(month) }) { Text("إنشاء ومشاركة PDF") }; OutlinedButton(Modifier.fillMaxWidth(), onClick = onExport) { Text("تصدير نسخة احتياطية") }; OutlinedButton(Modifier.fillMaxWidth(), onClick = onImport) { Text("استعادة نسخة احتياطية") } } }

@Composable private fun SettingsScreen(settings: AppSettings, onSave: (AppSettings) -> Unit) { var name by rememberSaveable { mutableStateOf(settings.employeeName) }; var salary by rememberSaveable { mutableStateOf(settings.defaultMonthlySalary.amount.toPlainString()) }; var days by rememberSaveable { mutableStateOf(settings.workingDaysPerMonth.toPlainString()) }; var hours by rememberSaveable { mutableStateOf(settings.workingHoursPerDay.toPlainString()) }; var multiplier by rememberSaveable { mutableStateOf(settings.defaultMultiplier.toPlainString()) }; Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) { Text("إعدادات الحساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("اسم المستخدم (اختياري)", name) { name = it }; NumberField("الراتب الشهري الافتراضي", salary) { salary = it }; NumberField("أيام العمل الشهرية", days) { days = it }; NumberField("ساعات العمل اليومية", hours) { hours = it }; NumberField("معامل الإضافي الافتراضي", multiplier) { multiplier = it }; Button(Modifier.fillMaxWidth(), onClick = { onSave(AppSettings(multiplier.toBigDecimalOrZero(), settings.currency, days.toBigDecimalOrZero(), hours.toBigDecimalOrZero(), Money(salary.toBigDecimalOrZero(), settings.currency), name)) }) { Text("حفظ الإعدادات") } } }

@Composable private fun NumberField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(Modifier.fillMaxWidth(), value, { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' || c in '٠'..'٩' }) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
@Composable private fun TextField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(Modifier.fillMaxWidth(), value, onChange, label = { Text(label) }, singleLine = true) }
private fun String.toEnglishDigits() = map { c -> if (c in '٠'..'٩') ('0'.code + c.code - '٠'.code).toChar() else c }.joinToString("")
private fun String.toBigDecimalOrZero() = runCatching { BigDecimal(toEnglishDigits().replace(',', '.')) }.getOrDefault(BigDecimal.ZERO)
private fun shareFile(context: Context, file: java.io.File, title: String) { val uri: Uri = com.overtime.worker.report.PdfReportGenerator.contentUri(context, file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, title)) }
