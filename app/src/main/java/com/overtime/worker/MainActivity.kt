package com.overtime.worker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.overtime.worker.ui.theme.OvertimeWorkerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OvertimeWorkerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OvertimeCalculatorScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun OvertimeCalculatorScreen() {
    var hourlyRate by remember { mutableStateOf("") }
    var overtimeHours by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf("1.5") }
    var result by remember { mutableStateOf<Double?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("حساب الإضافي للعمال", style = MaterialTheme.typography.headlineSmall)
        Text(
            "أدخل الأجر بالساعة وعدد ساعات الإضافي ومعامل الإضافي.",
            style = MaterialTheme.typography.bodyMedium
        )
        NumberField("الأجر بالساعة", hourlyRate) { hourlyRate = it }
        NumberField("ساعات الإضافي", overtimeHours) { overtimeHours = it }
        NumberField("معامل الإضافي", multiplier) { multiplier = it }
        Button(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
            onClick = {
                val rate = hourlyRate.toEnglishDouble()
                val hours = overtimeHours.toEnglishDouble()
                val factor = multiplier.toEnglishDouble()
                result = if (rate != null && hours != null && factor != null && rate >= 0 && hours >= 0 && factor >= 0) {
                    rate * hours * factor
                } else null
            }
        ) { Text("احسب") }
        result?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "قيمة الإضافي: ${String.format(Locale.US, "%.2f", it)}",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun String.toEnglishDouble(): Double? =
    replace('،', ',').replace(',', '.').replace(Regex("[٠-٩]")) { (it.value[0].code - '٠'.code).toString() }
        .toDoubleOrNull()
