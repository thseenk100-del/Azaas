package com.overtime.worker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overtime.worker.domain.model.OvertimeInput
import com.overtime.worker.domain.model.OvertimeResult
import com.overtime.worker.presentation.OvertimeViewModel
import com.overtime.worker.ui.theme.OvertimeWorkerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OvertimeWorkerTheme { OvertimeApp() } }
    }
}

private enum class AppTab(val title: String) { CALCULATOR("الحساب"), HISTORY("السجل"), SETTINGS("الإعدادات") }

@Composable
private fun OvertimeApp(viewModel: OvertimeViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CALCULATOR.name) }
    val tab = AppTab.valueOf(selectedTab)
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val result by viewModel.lastResult.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = { AppHeader(tab.title) },
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { selectedTab = item.name },
                            icon = { Text(item.title.take(1)) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
                when (tab) {
                    AppTab.CALCULATOR -> CalculatorScreen(settings.defaultMultiplier, result, error, viewModel::calculate)
                    AppTab.HISTORY -> HistoryScreen(history, viewModel::clearHistory)
                    AppTab.SETTINGS -> SettingsScreen(settings.defaultMultiplier, settings.currency, viewModel::updateSettings)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("كيف حسبت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CalculatorScreen(defaultMultiplier: Double, result: OvertimeResult?, error: String?, onCalculate: (OvertimeInput) -> Unit) {
    var rate by rememberSaveable { mutableStateOf("") }
    var regularHours by rememberSaveable { mutableStateOf("8") }
    var overtimeHours by rememberSaveable { mutableStateOf("") }
    var multiplier by rememberSaveable(defaultMultiplier) { mutableStateOf(defaultMultiplier.toString()) }
    var allowance by rememberSaveable { mutableStateOf("0") }
    var deductions by rememberSaveable { mutableStateOf("0") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("احسب مستحقاتك بدقة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("أدخل بيانات يوم العمل أو الفترة التي تريد حسابها. كل بياناتك تبقى على جهازك.", style = MaterialTheme.typography.bodyMedium)
        }
        item { NumberField("الأجر الأساسي بالساعة", rate) { rate = it } }
        item { NumberField("ساعات العمل النظامية", regularHours) { regularHours = it } }
        item { NumberField("ساعات العمل الإضافية", overtimeHours) { overtimeHours = it } }
        item { NumberField("معامل الإضافي", multiplier) { multiplier = it } }
        item { NumberField("البدلات (اختياري)", allowance) { allowance = it } }
        item { NumberField("الخصومات (اختياري)", deductions) { deductions = it } }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onCalculate(OvertimeInput(rate.toNumber(), regularHours.toNumber(), overtimeHours.toNumber(), multiplier.toNumber(), allowance.toNumber(), deductions.toNumber()))
                }
            ) { Text("احسب المستحقات") }
        }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        result?.let { calculated -> item { ResultCard(calculated) } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ResultCard(result: OvertimeResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("نتيجة الحساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ResultRow("الأجر النظامي", result.regularPay)
            ResultRow("قيمة الإضافي", result.overtimePay)
            ResultRow("الإجمالي قبل الخصم", result.grossPay)
            Divider()
            ResultRow("صافي المستحق", result.netPay, emphasized = true)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Double, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(formatMoney(value), fontWeight = FontWeight.Bold, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HistoryScreen(history: List<com.overtime.worker.domain.model.CalculationRecord>, onClear: () -> Unit) {
    if (history.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("لا توجد حسابات محفوظة بعد", style = MaterialTheme.typography.titleMedium)
            Text("ستظهر نتائجك هنا تلقائيًا بعد إجراء أول عملية حساب.", modifier = Modifier.padding(top = 8.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("آخر الحسابات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onClear) { Text("مسح السجل") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(record.date)
                                Text(formatMoney(record.result.netPay), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text("إضافي: ${record.input.overtimeHours.formatNumber()} ساعة × ${record.input.overtimeMultiplier.formatNumber()}")
                            Text("الأجر بالساعة: ${formatMoney(record.input.hourlyRate)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(defaultMultiplier: Double, currency: String, onSave: (Double, String) -> Unit) {
    var multiplier by rememberSaveable(defaultMultiplier) { mutableStateOf(defaultMultiplier.toString()) }
    var currencyValue by rememberSaveable(currency) { mutableStateOf(currency) }
    var saved by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("الإعدادات المحلية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("خصص القيم الافتراضية التي يستخدمها التطبيق. لا يتم إرسال أي بيانات خارج الهاتف.")
        NumberField("معامل الإضافي الافتراضي", multiplier) { multiplier = it }
        OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = currencyValue, onValueChange = { currencyValue = it }, label = { Text("رمز العملة") }, singleLine = true)
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onSave(multiplier.toNumber(), currencyValue); saved = true }) { Text("حفظ الإعدادات") }
        if (saved) Text("تم حفظ الإعدادات على جهازك.", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(), value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' || it == ',' || it in '٠'..'٩' }) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun String.toNumber(): Double = replace('،', '.').replace(',', '.').mapArabicDigits().toDoubleOrNull() ?: -1.0
private fun String.mapArabicDigits(): String = map { char -> if (char in '٠'..'٩') ('0'.code + char.code - '٠'.code).toChar() else char }.joinToString("")
private fun Double.formatNumber(): String = String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
private fun formatMoney(value: Double): String = "${String.format(Locale.US, "%.2f", value)} ر.س"
