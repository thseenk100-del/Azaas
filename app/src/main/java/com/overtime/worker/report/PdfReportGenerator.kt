package com.overtime.worker.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.overtime.worker.domain.model.CalculationRecord
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfReportGenerator {
    fun createMonthlyReport(context: Context, month: String, records: List<CalculationRecord>, currency: String): File {
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(directory, "how-counted-$month.pdf")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f }
        val titlePaint = Paint(paint).apply { textSize = 20f; isFakeBoldText = true }
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        var y = 50f
        canvas.drawText("كيف حسبت - تقرير $month", 380f, y, titlePaint); y += 32
        canvas.drawText("عدد السجلات: ${records.size}", 380f, y, paint); y += 22
        val totalHours = records.sumOf { it.input.overtimeHours }
        val totalNet = records.sumOf { it.result.netPay }
        canvas.drawText("إجمالي الإضافي: ${String.format(Locale.US, "%.2f", totalHours)} ساعة", 380f, y, paint); y += 22
        canvas.drawText("صافي المستحق: ${String.format(Locale.US, "%.2f", totalNet)} $currency", 380f, y, paint); y += 30
        records.take(25).forEach { record ->
            canvas.drawText("${record.date} | إضافي: ${record.input.overtimeHours} | صافي: ${String.format(Locale.US, "%.2f", record.result.netPay)} $currency", 550f, y, paint)
            y += 21
        }
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun contentUri(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
