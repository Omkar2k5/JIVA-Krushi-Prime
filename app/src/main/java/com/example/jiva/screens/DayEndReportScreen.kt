package com.example.jiva.screens

// Redesigned from scratch to strictly reflect DayEndReport API fields
// API fields used: cmpCode, dayDate, purchase, sale, cash_Opening, cash_Closing, bank_Opening, bank_Closing, expenses, payments, receipts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.R
import com.example.jiva.data.repository.JivaRepositoryImpl
import com.example.jiva.viewmodel.DayEndReportUiState
import com.example.jiva.viewmodel.DayEndReportViewModel
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import android.os.Environment

// Restored for DataMapper compatibility
// Used by DataMapper.calculateDayEndData
data class DayEndData(
    val totalSale: Double,
    val totalPurchase: Double,
    val cashReceived: Double,
    val cashInHand: Double,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEndReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val database = (context.applicationContext as JivaApplication).database

    // Repo + VM
    val jivaRepository = remember { JivaRepositoryImpl(database) }
    val viewModel: DayEndReportViewModel = viewModel { DayEndReportViewModel(jivaRepository) }

    // State
    val uiState by viewModel.uiState.collectAsState()

    // Formatters
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Date input (yyyy-MM-dd) — aligns with API contract
    var dateIso by remember { mutableStateOf(uiState.selectedDayDate) }
    LaunchedEffect(dateIso) { viewModel.updateSelectedDate(dateIso) }

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis = remember(uiState.selectedDayDate) { isoToMillis(uiState.selectedDayDate) ?: System.currentTimeMillis() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val sel = datePickerState.selectedDateMillis
                    if (sel != null) {
                        dateIso = millisToIso(sel) // yyyy-MM-dd
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Toast errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Status bar height
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top?.toDp()
            ?: 24.dp
    }

    if (uiState.isLoading) {
        // Keep initial loading (from DB bootstrap) though we do not render DB metrics anymore
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JivaColors.LightGray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = JivaColors.DeepBlue)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(JivaColors.DeepBlue, JivaColors.Purple)
                    )
                )
                .padding(top = statusBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(JivaColors.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JivaColors.White
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "JIVA Logo",
                        modifier = Modifier
                            .padding(start = 8.dp, end = 12.dp)
                            .size(32.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Day End Report",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White
                        )
                        // Show only fields aligned with API: Company (cmpCode) and selected Day (dayDate)
                        val companyStr = uiState.companyCode.toString()
                        Text(
                            text = "Company: $companyStr | Date: ${uiState.selectedDayDate}",
                            fontSize = 13.sp,
                            color = JivaColors.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date and Fetch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { /* read-only; use picker */ },
                        readOnly = true,
                        label = { Text("Day Date (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick date", tint = JivaColors.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JivaColors.White,
                            unfocusedTextColor = JivaColors.White,
                            focusedBorderColor = JivaColors.White,
                            unfocusedBorderColor = JivaColors.White.copy(alpha = 0.6f),
                            cursorColor = JivaColors.White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.fetchDayEndInfo(context) },
                        enabled = !uiState.isDayEndLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = JivaColors.White)
                    ) {
                        if (uiState.isDayEndLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = JivaColors.DeepBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Fetch",
                                tint = JivaColors.DeepBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch", color = JivaColors.DeepBlue)
                        }
                    }
                }
            }
        }

        // Content — strictly API fields
        DayEndReportContent(uiState, currencyFormatter)
    }
}

@Composable
private fun DayEndReportContent(uiState: DayEndReportUiState, currencyFormatter: NumberFormat) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Make screen vertically scrollable so bottom content is fully accessible
    androidx.compose.foundation.rememberScrollState().let { scrollState ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top action row with Share PDF like Price report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { scope.launch { generateAndShareDayEndPDF(context, uiState, currencyFormatter) } },
                    colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Purple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Share PDF")
                    Spacer(Modifier.width(8.dp))
                    Text("Share PDF")
                }
            }
            // Key financial tiles (Sale, Purchase, Receipts, Payments, Expenses)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Sale",
                    value = currencyFormatter.format(uiState.dayEndSale),
                    icon = Icons.Default.TrendingUp,
                    backgroundColor = JivaColors.Green,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Purchase",
                    value = currencyFormatter.format(uiState.dayEndPurchase),
                    icon = Icons.Default.ShoppingCart,
                    backgroundColor = JivaColors.Orange,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Receipts",
                    value = currencyFormatter.format(uiState.dayEndReceipts),
                    icon = Icons.Default.ArrowDownward,
                    backgroundColor = JivaColors.LightBlue,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Payments",
                    value = currencyFormatter.format(uiState.dayEndPayments),
                    icon = Icons.Default.ArrowUpward,
                    backgroundColor = JivaColors.Red,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Expenses",
                    value = currencyFormatter.format(uiState.dayEndExpenses),
                    icon = Icons.Default.Receipt,
                    backgroundColor = Color(0xFF795548),
                    modifier = Modifier.weight(1f)
                )
            }

            // Cash & Bank opening/closing — single-line responsive pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricPill(
                    label = "Cash Opening",
                    value = currencyFormatter.format(uiState.dayEndCashOpening),
                    color = JivaColors.Purple
                )
                MetricPill(
                    label = "Cash Closing",
                    value = currencyFormatter.format(uiState.dayEndCashClosing),
                    color = JivaColors.Purple
                )
                MetricPill(
                    label = "Bank Opening",
                    value = currencyFormatter.format(uiState.dayEndBankOpening),
                    color = JivaColors.Teal
                )
                MetricPill(
                    label = "Bank Closing",
                    value = currencyFormatter.format(uiState.dayEndBankClosing),
                    color = JivaColors.Teal
                )
            }

            // Charts — simple, readable, and responsive
            ChartCard(
                title = "Sales vs Purchase",
                modifier = Modifier.fillMaxWidth()
            ) {
                val items = listOf(
                    "Sale" to uiState.dayEndSale,
                    "Purchase" to uiState.dayEndPurchase
                )
                BarChart(values = items, barColors = listOf(JivaColors.Green, JivaColors.Orange))
            }

            ChartCard(
                title = "Receipts • Payments • Expenses",
                modifier = Modifier.fillMaxWidth()
            ) {
                val parts = listOf(
                    SegmentPart("Receipts", uiState.dayEndReceipts, JivaColors.LightBlue),
                    SegmentPart("Payments", uiState.dayEndPayments, JivaColors.Red),
                    SegmentPart("Expenses", uiState.dayEndExpenses, Color(0xFF795548))
                )
                SegmentedBar(parts = parts)
            }

            ChartCard(
                title = "Closing Position",
                modifier = Modifier.fillMaxWidth()
            ) {
                val items = listOf(
                    "Cash Closing" to uiState.dayEndCashClosing,
                    "Bank Closing" to uiState.dayEndBankClosing
                )
                BarChart(values = items, barColors = listOf(JivaColors.Purple, JivaColors.Teal))
            }

            // Bottom Share PDF button like Price report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { scope.launch { generateAndShareDayEndPDF(context, uiState, currencyFormatter) } },
                    colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Purple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Share PDF")
                    Spacer(Modifier.width(8.dp))
                    Text("SHARE PDF REPORT")
                }
            }
        }
    }
}

// ---- UI Components ----

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = backgroundColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "$label:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BarChart(
    values: List<Pair<String, Double>>,
    barColors: List<Color>,
    maxBarHeight: Dp = 160.dp,
    barWidth: Dp = 26.dp,
    barCornerRadius: Dp = 8.dp,
    gap: Dp = 20.dp
) {
    val maxValue = values.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, (label, rawValue) ->
            val value = rawValue.coerceAtLeast(0.0)
            val heightFraction = (value / maxValue).toFloat()
            val color = barColors.getOrElse(index) { JivaColors.DeepBlue }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = gap / 4)) {
                // Bar
                Box(
                    modifier = Modifier
                        .height(maxBarHeight)
                        .width(barWidth),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Bar background track
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(barCornerRadius))
                            .background(Color(0xFFEFEFEF))
                    )
                    // Bar fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(barCornerRadius))
                            .background(color)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text(
                    text = shortNumber(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private data class SegmentPart(val label: String, val value: Double, val color: Color)

@Composable
private fun SegmentedBar(parts: List<SegmentPart>, height: Dp = 22.dp) {
    val safeParts = parts.map { it.copy(value = it.value.coerceAtLeast(0.0)) }
    val total = safeParts.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
    val positiveParts = safeParts.filter { it.value > 0 }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(100))
                .background(Color(0xFFEFEFEF))
        ) {
            positiveParts.forEach { part ->
                val fraction = (part.value / total).toFloat()
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(fraction)
                            .background(part.color)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            positiveParts.forEach { part ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(part.color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = part.label, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(text = shortNumber(part.value), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun LineDivider(color: Color = Color(0xFFE8E8E8), thickness: Dp = 1.dp) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(thickness)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = thickness.toPx()
        )
    }
}

private fun shortNumber(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1_00_00_000 -> String.format(Locale.getDefault(), "%.1fCr", v / 1_00_00_000) // Crore
        abs >= 1_00_000 -> String.format(Locale.getDefault(), "%.1fL", v / 1_00_000) // Lakh
        abs >= 1_000 -> String.format(Locale.getDefault(), "%.1fk", v / 1_000)
        else -> String.format(Locale.getDefault(), "%.0f", v)
    }
}

private fun isoToMillis(iso: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.parse(iso)?.time
    } catch (_: ParseException) {
        null
    }
}

private fun millisToIso(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

// --- PDF generation (tabular/sectioned like Price report) ---
private suspend fun generateAndShareDayEndPDF(
    context: android.content.Context,
    uiState: DayEndReportUiState,
    currencyFormatter: NumberFormat
) {
    withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f
            val contentWidth = pageWidth - (2 * margin)
            val pdf = PdfDocument()

            val titlePaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val headerPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
            }
            val borderPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }

            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            var y = margin

            // Title
            canvas.drawText("Day End Report", (pageWidth / 2).toFloat(), y, titlePaint); y += 24f
            canvas.drawText("Company: ${uiState.companyCode}  |  Date: ${uiState.selectedDayDate}", (pageWidth / 2).toFloat(), y, textPaint); y += 24f

            // Summary grid (Sale, Purchase, Receipts, Payments, Expenses)
            val summary = listOf(
                "Sale" to uiState.dayEndSale,
                "Purchase" to uiState.dayEndPurchase,
                "Receipts" to uiState.dayEndReceipts,
                "Payments" to uiState.dayEndPayments,
                "Expenses" to uiState.dayEndExpenses
            )
            canvas.drawText("Summary", margin, y, headerPaint); y += 16f

            val colW = contentWidth / 2
            summary.chunked(2).forEach { row ->
                var x = margin
                val rowBottom = y + 18f
                row.forEach { (label, value) ->
                    val rect = android.graphics.RectF(x, y, x + colW, rowBottom)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText("$label: ${currencyFormatter.format(value)}", x + 8f, rowBottom - 6f, textPaint)
                    x += colW
                }
                y = rowBottom
            }
            y += 16f

            // Cash/Bank opening/closing
            canvas.drawText("Cash / Bank", margin, y, headerPaint); y += 16f
            val cashBank = listOf(
                "Cash Opening" to uiState.dayEndCashOpening,
                "Cash Closing" to uiState.dayEndCashClosing,
                "Bank Opening" to uiState.dayEndBankOpening,
                "Bank Closing" to uiState.dayEndBankClosing
            )
            cashBank.chunked(2).forEach { row ->
                var x = margin
                val rowBottom = y + 18f
                row.forEach { (label, value) ->
                    val rect = android.graphics.RectF(x, y, x + colW, rowBottom)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText("$label: ${currencyFormatter.format(value)}", x + 8f, rowBottom - 6f, textPaint)
                    x += colW
                }
                y = rowBottom
            }

            pdf.finishPage(page)

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "DayEnd_Report_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.pdf"
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "PDF saved to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
                try {
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Day End Report")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Day End Report attached.")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Day End Report"))
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "PDF saved but sharing failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
