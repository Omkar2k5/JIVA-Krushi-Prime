package com.example.jiva.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PDFGenerator {
    
    data class TableColumn(
        val header: String,
        val width: Float,
        val getValue: (Any) -> String
    )
    
    data class PDFConfig(
        val title: String,
        val fileName: String,
        val columns: List<TableColumn>,
        val data: List<Any>,
        val totalRow: Map<String, String>? = null
    )
    
    suspend fun generateAndSharePDF(
        context: Context,
        config: PDFConfig
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Create PDF document
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                // Paint objects for different text styles
                val titlePaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                
                val headerPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }
                
                val cellPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10f
                    typeface = Typeface.DEFAULT
                }
                
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                
                // Draw title
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                canvas.drawText(config.title, 297.5f, 50f, titlePaint)
                canvas.drawText("Generated on: $currentDate", 297.5f, 75f, cellPaint)
                
                // Table dimensions
                val startX = 30f
                val startY = 120f
                val rowHeight = 25f
                val colWidths = config.columns.map { it.width }.toFloatArray()
                val totalWidth = colWidths.sum()
                
                // Draw table headers
                var currentX = startX
                var currentY = startY
                
                // Header row background
                val headerRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                canvas.drawRect(headerRect, Paint().apply { 
                    color = android.graphics.Color.LTGRAY
                    style = Paint.Style.FILL 
                })
                
                // Draw header text and borders
                for (i in config.columns.indices) {
                    val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(config.columns[i].header, currentX + 5f, currentY + 15f, headerPaint)
                    currentX += colWidths[i]
                }
                
                // Draw data rows (limit to fit on page)
                currentY += rowHeight
                val maxRows = minOf(config.data.size, 25) // Limit to 25 rows to fit on page
                
                for (i in 0 until maxRows) {
                    val item = config.data[i]
                    currentX = startX
                    
                    for (j in config.columns.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[j], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        
                        val cellValue = config.columns[j].getValue(item)
                        val truncatedValue = if (cellValue.length > 15) cellValue.take(12) + "..." else cellValue
                        canvas.drawText(truncatedValue, currentX + 5f, currentY + 15f, cellPaint)
                        currentX += colWidths[j]
                    }
                    currentY += rowHeight
                }
                
                // Draw totals row if provided
                config.totalRow?.let { totals ->
                    currentX = startX
                    val totalRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                    canvas.drawRect(totalRect, Paint().apply { 
                        color = android.graphics.Color.CYAN
                        style = Paint.Style.FILL
                        alpha = 100 
                    })
                    
                    for (i in config.columns.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        
                        val cellValue = totals[config.columns[i].header] ?: ""
                        canvas.drawText(cellValue, currentX + 5f, currentY + 15f, headerPaint)
                        currentX += colWidths[i]
                    }
                }
                
                // Add footer
                canvas.drawText("Total Entries: ${config.data.size}", startX, currentY + 50f, cellPaint)
                if (maxRows < config.data.size) {
                    canvas.drawText("Showing first $maxRows entries", startX, currentY + 70f, cellPaint)
                }
                canvas.drawText("Generated by JIVA App", startX, currentY + 90f, cellPaint)
                
                pdfDocument.finishPage(page)
                
                // Save PDF to external storage
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${config.fileName}_$timestamp.pdf"
                val file = File(downloadsDir, fileName)
                
                val fileOutputStream = FileOutputStream(file)
                pdfDocument.writeTo(fileOutputStream)
                fileOutputStream.close()
                pdfDocument.close()
                
                // Show success message and share
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                    sharePDF(context, file, config.title)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun sharePDF(context: Context, file: File, reportTitle: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, reportTitle)
                putExtra(Intent.EXTRA_TEXT, "Please find the $reportTitle attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share $reportTitle")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
