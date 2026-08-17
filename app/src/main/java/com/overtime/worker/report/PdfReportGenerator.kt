package com.overtime.worker.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.overtime.worker.domain.model.OvertimeRecord
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal

object PdfReportGenerator {
    fun createMonthlyReport(context: Context, month: String, records: List<OvertimeRecord>, currency: String): File {
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(directory, "how-counted-overtime-$month.pdf")
        val document = PdfDocument(); val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f }
        val titlePaint = Paint(paint).apply { textSize = 20f; isFakeBoldText = true }
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val canvas = page.canvas; var y = 50f
        canvas.drawText("كيف حسبت - تقرير الإضافي $month", 380f, y, titlePaint); y += 32
        canvas.drawText("عدد السجلات: ${records.size}", 380f, y, paint); y += 22
        val totalOvertimeHours = records.fold(BigDecimal.ZERO) { total, record -> total + record.overtimeHours }
        val totalOvertimePay = records.fold(BigDecimal.ZERO) { total, record -> total + record.overtimePay.amount }
        canvas.drawText("إجمالي ساعات الإضافي: ${totalOvertimeHours.stripTrailingZeros().toPlainString()}", 380f, y, paint); y += 22
        canvas.drawText("إجمالي قيمة الإضافي: ${totalOvertimePay.stripTrailingZeros().toPlainString()} $currency", 380f, y, paint); y += 30
        records.take(25).forEach { record ->
            canvas.drawText("${record.date} | ${record.overtimeHours} ساعة | ${record.overtimePay.amount} $currency", 550f, y, paint); y += 21
        }
        document.finishPage(page); FileOutputStream(file).use { document.writeTo(it) }; document.close(); return file
    }

    fun contentUri(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
