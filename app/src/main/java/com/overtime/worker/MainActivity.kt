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
import androidx.compose.material3.Divider
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overtime.worker.domain.model.AppSettings
import com.overtime.worker.domain.model.CalculationRecord
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import com.overtime.worker.report.PdfReportGenerator
import com.overtime.worker.presentation.OvertimeViewModel
import com.overtime.worker.ui.theme.OvertimeWorkerTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { OvertimeWorkerTheme { OvertimeApp() } } }
}

private enum class AppTab(val title: String) { CALCULATOR("الحساب"), HISTORY("السجل"), REPORTS("التقارير"), SETTINGS("الإعدادات") }

@Composable
private fun OvertimeApp(viewModel: OvertimeViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CALCULATOR.name) }
    val tab = AppTab.valueOf(selectedTab)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val result by viewModel.lastResult.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val history by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val allHistory by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.exportBackup().toByteArray()) } } }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { viewModel.restoreBackup(it.readText()) } } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(topBar = { AppHeader(tab.title) }, bottomBar = {
            NavigationBar { AppTab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { selectedTab = item.name }, icon = { Text(item.title.take(1)) }, label = { Text(item.title) }) } }
        }) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
                when (tab) {
                    AppTab.CALCULATOR -> CalculatorScreen(settings, result, error, message, viewModel::calculate)
                    AppTab.HISTORY -> HistoryScreen(history, viewModel::setQuery, viewModel::setMonth, viewModel::delete, viewModel::clearHistory)
                    AppTab.REPORTS -> ReportsScreen(allHistory, settings, onExport = { backupExporter.launch("كيف-حسبت-${LocalDate.now()}.json") }, onImport = { backupImporter.launch(arrayOf("application/json", "text/plain")) }, onPdf = { month ->
                        scope.launch { val file = PdfReportGenerator.createMonthlyReport(context, month, allHistory.filter { it.date.startsWith(month) }, settings.currency); shareFile(context, file, "تقرير كيف حسبت") }
                    })
                    AppTab.SETTINGS -> SettingsScreen(settings, viewModel::saveSettings)
                }
            }
        }
    }
}

@Composable private fun AppHeader(title: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) { Text("كيف حسبت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(title, color = MaterialTheme.colorScheme.primary) } }

@Composable
private fun CalculatorScreen(settings: AppSettings, result: OvertimeResult?, error: String?, message: String?, onCalculate: (OvertimeInput, String, String) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var rate by rememberSaveable(settings.defaultHourlyRate) { mutableStateOf(if (settings.defaultHourlyRate > 0) settings.defaultHourlyRate.toString() else "") }
    var regularHours by rememberSaveable(settings.standardDailyHours) { mutableStateOf(settings.standardDailyHours.toString()) }
    var overtimeHours by rememberSaveable { mutableStateOf("") }
    var multiplier by rememberSaveable(settings.defaultMultiplier) { mutableStateOf(settings.defaultMultiplier.toString()) }
    var allowance by rememberSaveable { mutableStateOf("0") }
    var deductions by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { Spacer(Modifier.height(4.dp)); Text("تسجيل يوم عمل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("السياسات غير المحددة مسبقًا قابلة للتعديل من الإعدادات أو لكل عملية.") }
        item { TextField("التاريخ (YYYY-MM-DD)", date) { date = it } }
        item { NumberField("الأجر الأساسي بالساعة", rate) { rate = it } }
        item { NumberField("ساعات العمل الأساسية", regularHours) { regularHours = it } }
        item { NumberField("ساعات العمل الإضافية", overtimeHours) { overtimeHours = it } }
        item { NumberField("معامل/نسبة الإضافي", multiplier) { multiplier = it } }
        item { NumberField("البدلات", allowance) { allowance = it } }
        item { NumberField("الخصومات", deductions) { deductions = it } }
        item { TextField("ملاحظة أو نوع العمل", note) { note = it } }
        item { Button(Modifier.fillMaxWidth(), onClick = { onCalculate(OvertimeInput(rate.toNumber(), regularHours.toNumber(), overtimeHours.toNumber(), multiplier.toNumber(), allowance.toNumber(), deductions.toNumber()), date, note) }) { Text("احسب واحفظ") } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }; message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }; result?.let { item { ResultCard(it, settings.currency) } }; item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable private fun ResultCard(result: OvertimeResult, currency: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("نتيجة الحساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); ResultRow("الأجر الأساسي", result.regularPay, currency); ResultRow("قيمة الإضافي", result.overtimePay, currency); ResultRow("الإجمالي", result.grossPay, currency); Divider(); ResultRow("الصافي المستحق", result.netPay, currency, true) } } }
@Composable private fun ResultRow(label: String, value: Double, currency: String, emphasized: Boolean = false) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(label, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal); Text("${formatMoney(value)} $currency", color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) } }

@Composable
private fun HistoryScreen(records: List<CalculationRecord>, onQuery: (String) -> Unit, onMonth: (String) -> Unit, onDelete: (CalculationRecord) -> Unit, onClear: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var month by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Text("السجل الكامل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("بحث بالملاحظة أو التاريخ", query) { query = it; onQuery(it) }; TextField("الشهر YYYY-MM", month) { month = it; onMonth(it) }; Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween) { Text("${records.size} عملية"); OutlinedButton(onClick = onClear) { Text("مسح الكل") } }
        if (records.isEmpty()) Text("لا توجد سجلات مطابقة.", modifier = Modifier.padding(top = 30.dp)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(records, key = { it.id }) { record -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), Arrangement.spacedBy(5.dp)) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(record.date); Text(formatMoney(record.result.netPay), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }; Text("إضافي: ${record.input.overtimeHours.formatNumber()} ساعة × ${record.input.overtimeMultiplier.formatNumber()}"); if (record.note.isNotBlank()) Text(record.note); OutlinedButton(onClick = { onDelete(record) }) { Text("حذف") } } } } } }
}

@Composable
private fun ReportsScreen(records: List<CalculationRecord>, settings: AppSettings, onExport: () -> Unit, onImport: () -> Unit, onPdf: (String) -> Unit) {
    var month by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }; val monthly = records.filter { it.date.startsWith(month) }; val hours = monthly.sumOf { it.input.overtimeHours }; val net = monthly.sumOf { it.result.netPay }
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) { Text("التقارير الشهرية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("الشهر YYYY-MM", month) { month = it }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(7.dp)) { Text("ملخص $month", fontWeight = FontWeight.Bold); Text("عدد العمليات: ${monthly.size}"); Text("إجمالي ساعات الإضافي: ${hours.formatNumber()}"); Text("إجمالي الصافي: ${formatMoney(net)} ${settings.currency}") } }; Button(Modifier.fillMaxWidth(), onClick = { onPdf(month) }) { Text("إنشاء ومشاركة PDF") }; OutlinedButton(Modifier.fillMaxWidth(), onClick = onExport) { Text("تصدير نسخة احتياطية") }; OutlinedButton(Modifier.fillMaxWidth(), onClick = onImport) { Text("استعادة نسخة احتياطية") } }
}

@Composable
private fun SettingsScreen(settings: AppSettings, onSave: (AppSettings) -> Unit) { var name by rememberSaveable(settings.employeeName) { mutableStateOf(settings.employeeName) }; var rate by rememberSaveable(settings.defaultHourlyRate) { mutableStateOf(settings.defaultHourlyRate.toString()) }; var hours by rememberSaveable(settings.standardDailyHours) { mutableStateOf(settings.standardDailyHours.toString()) }; var multiplier by rememberSaveable(settings.defaultMultiplier) { mutableStateOf(settings.defaultMultiplier.toString()) }; var currency by rememberSaveable(settings.currency) { mutableStateOf(settings.currency) }; Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) { Text("الإعدادات الأساسية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextField("اسم العامل (اختياري)", name) { name = it }; NumberField("الأجر الافتراضي بالساعة", rate) { rate = it }; NumberField("ساعات الدوام الأساسية", hours) { hours = it }; NumberField("معامل الإضافي الافتراضي", multiplier) { multiplier = it }; TextField("رمز العملة", currency) { currency = it }; Button(Modifier.fillMaxWidth(), onClick = { onSave(AppSettings(multiplier.toNumber(), currency.ifBlank { "ر.س" }, hours.toNumber(), rate.toNumber(), name)) }) { Text("حفظ الإعدادات") } }
}

@Composable private fun NumberField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(Modifier.fillMaxWidth(), value, { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' || c in '٠'..'٩' }) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
@Composable private fun TextField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(Modifier.fillMaxWidth(), value, onChange, label = { Text(label) }, singleLine = true) }
private fun String.toNumber(): Double = replace('،', '.').replace(',', '.').mapArabicDigits().toDoubleOrNull() ?: -1.0
private fun String.mapArabicDigits() = map { c -> if (c in '٠'..'٩') ('0'.code + c.code - '٠'.code).toChar() else c }.joinToString("")
private fun Double.formatNumber() = String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
private fun formatMoney(value: Double) = String.format(Locale.US, "%.2f", value)
private fun shareFile(context: Context, file: java.io.File, title: String) { val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, title)) }
