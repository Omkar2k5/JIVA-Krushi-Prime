package com.example.jiva.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.jiva.components.ReportLoading
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.LedgerReportViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

// PDF generation utils
import com.example.jiva.utils.PDFGenerator



/**
 * Ledger Entry data class - Complete with all API fields
 */
data class LedgerEntry(
    val entryNo: String,
    val manualNo: String,
    val srNo: String,
    val entryType: String,
    val entryDate: String,
    val refNo: String,
    val acId: String,
    val dr: String,
    val cr: String,
    val narration: String,
    val isClere: String,
    val trascType: String,
    val gstRate: String,
    val amt: String,
    val igst: String
)

// Top-level PDF helpers for Ledger report
private fun drawTextCentered(
    canvas: android.graphics.Canvas,
    text: String,
    centerX: Float,
    y: Float,
    maxWidth: Float,
    paint: android.graphics.Paint
) {
    val ellipsis = "…"
    val maxLen = paint.breakText(text, true, maxWidth - 6f, null)
    val toDraw = if (maxLen < text.length && maxLen > 1) text.substring(0, maxLen - 1) + ellipsis else text
    canvas.drawText(toDraw, centerX - paint.measureText(toDraw) / 2, y, paint)
}

private fun shareLedgerPDF(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, com.example.jiva.utils.UserEnv.getCompanyName(context) ?: "Ledger Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Ledger Report attached.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Ledger Report").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Suppress("FunctionName")
private suspend fun generateAndShareLedgerPDF(
    context: android.content.Context,
    entries: List<LedgerEntry>,
    totalDr: Double,
    totalCr: Double,
    closingBalance: Double
) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val pageWidth = 842
            val pageHeight = 595
            val margin = 20f
            val contentWidth = pageWidth - (2 * margin)
            val bottomY = pageHeight - margin

            val pdfDocument = android.graphics.pdf.PdfDocument()

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 8f
                typeface = android.graphics.Typeface.DEFAULT
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            val fillHeaderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = android.graphics.Paint.Style.FILL
            }

            val headers = listOf("Entry No", "Manual No", "Type", "Date", "Ref No", "AC ID", "DR", "CR", "Narration")
            val weights = floatArrayOf(0.08f, 0.08f, 0.10f, 0.10f, 0.08f, 0.08f, 0.10f, 0.10f, 0.28f)
            val weightSum = weights.sum()
            val normalized = weights.map { it / weightSum }
            val colWidths = FloatArray(headers.size) { i -> (contentWidth * normalized[i]).coerceAtLeast(60f) }

            val titleBlockHeight = 30f + 20f + 15f + 15f + 25f  // Added 15f for owner name
            val headerHeight = 24f
            val rowHeight = 18f
            val availableHeightBase = pageHeight - titleBlockHeight - headerHeight - margin - margin
            val rowsPerPage = (availableHeightBase / rowHeight).toInt().coerceAtLeast(1)

            var currentPage = 1
            var entryIndex = 0
            val totalPages = ((entries.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

            val companyName = com.example.jiva.utils.UserEnv.getCompanyName(context) ?: "Ledger Report"
            val ownerName = com.example.jiva.utils.UserEnv.getOwnerName(context)

            while (entryIndex < entries.size || (entries.isEmpty() && currentPage == 1)) {
                val page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create())
                val canvas = page.canvas
                var currentY = 30f

                canvas.drawText(companyName, (pageWidth / 2).toFloat(), currentY, titlePaint)
                currentY += 20f
                
                // Add owner name below company name with medium font size
                if (!ownerName.isNullOrBlank()) {
                    val ownerPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 12f  // Medium font size (between company name 18f and table data 8f)
                        typeface = android.graphics.Typeface.DEFAULT
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.drawText(ownerName, (pageWidth / 2).toFloat(), currentY, ownerPaint)
                    currentY += 15f
                }

                currentY += 15f
                canvas.drawText("Page $currentPage of $totalPages", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 25f

                var xCursor = margin
                val headerTop = currentY
                val headerBottom = currentY + headerHeight
                for (i in headers.indices) {
                    val rect = android.graphics.RectF(xCursor, headerTop, xCursor + colWidths[i], headerBottom)
                    canvas.drawRect(rect, fillHeaderPaint)
                    canvas.drawRect(rect, borderPaint)
                    drawTextCentered(canvas, headers[i], xCursor + colWidths[i] / 2, headerBottom - 6f, colWidths[i] - 10f, headerPaint)
                    xCursor += colWidths[i]
                }
                currentY = headerBottom

                val endIndex = (entryIndex + rowsPerPage).coerceAtMost(entries.size)
                for (i in entryIndex until endIndex) {
                    if (currentY + rowHeight > bottomY) break
                    val e = entries[i]
                    xCursor = margin
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight
                    val cells = listOf(
                        e.entryNo,
                        e.manualNo,
                        e.entryType,
                        e.entryDate,
                        e.refNo,
                        e.acId,
                        "₹${e.dr}",
                        "₹${e.cr}",
                        e.narration
                    )
                    for (j in cells.indices) {
                        val rect = android.graphics.RectF(xCursor, rowTop, xCursor + colWidths[j], rowBottom)
                        canvas.drawRect(rect, borderPaint)
                        drawTextCentered(canvas, cells[j], xCursor + colWidths[j] / 2, rowBottom - 4f, colWidths[j] - 10f, cellPaint)
                        xCursor += colWidths[j]
                    }
                    currentY = rowBottom
                }

                if (currentPage == totalPages && currentY + rowHeight <= bottomY) {
                    val totalTop = currentY + 8f
                    val totalBottom = totalTop + rowHeight
                    val totalRect = android.graphics.RectF(margin, totalTop, margin + contentWidth, totalBottom)
                    canvas.drawRect(totalRect, fillHeaderPaint)
                    canvas.drawRect(totalRect, borderPaint)
                    drawTextCentered(
                        canvas,
                        "Totals — DR: ₹${String.format("%.2f", totalDr)}  CR: ₹${String.format("%.2f", totalCr)}  Closing: ₹${String.format("%.2f", closingBalance)}",
                        margin + contentWidth / 2,
                        totalBottom - 4f,
                        contentWidth - 10f,
                        headerPaint
                    )
                }

                pdfDocument.finishPage(page)
                entryIndex = endIndex
                currentPage++
            }

            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Ledger_Report_$timestamp.pdf"
            val file = java.io.File(downloadsDir, fileName)

            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "PDF saved to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
                shareLedgerPDF(context, file)
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication

    // Create the ViewModel with the repository
    val viewModel: LedgerReportViewModel = viewModel(
        factory = LedgerReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management
    var entryTypeFilter by remember { mutableStateOf("All Types") }
    var dateFromSearch by remember { mutableStateOf("") }
    var dateToSearch by remember { mutableStateOf("") }
    var narrationSearch by remember { mutableStateOf("") }
    var isEntryTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Account Opening fields (static for now; will be set via API later)
    var openingDr by remember { mutableStateOf("0") }
    var openingCr by remember { mutableStateOf("0") }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading: fetch Account Names on open
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Fetching account names..."

        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull() ?: 1017
        try {
            viewModel.loadAccountNames(userId, year)
        } catch (_: Exception) { }

        // Smooth progress animation (brief)
        for (i in 0..100 step 25) {
            loadingProgress = i
            dataLoadingProgress = i.toFloat()
            loadingMessage = if (i < 100) "Preparing data..." else "Complete!"
            kotlinx.coroutines.delay(40)
        }
        isScreenLoading = false
    }

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Data sources
    val ledgerEntities by viewModel.observeLedger(year).collectAsState(initial = emptyList())
    val serverApiItems by viewModel.serverLedgerItems.collectAsState()

    // Prefer server filtered data when present, otherwise use Room DB
    val allEntries = remember(ledgerEntities, serverApiItems) {
        try {
            if (serverApiItems.isNotEmpty()) {
                serverApiItems.map { item ->
                    LedgerEntry(
                        entryNo = item.entryNo,
                        manualNo = item.manualNo,
                        srNo = item.srNO,
                        entryType = item.entryType,
                        entryDate = item.entryDate,
                        refNo = item.refNo,
                        acId = item.ac_ID,
                        dr = item.dr,
                        cr = item.cr,
                        narration = item.narration,
                        isClere = item.isClere,
                        trascType = item.trascType,
                        gstRate = item.gstRate,
                        amt = item.amt,
                        igst = item.igst
                    )
                }
            } else {
                ledgerEntities.map { entity ->
                    LedgerEntry(
                        entryNo = entity.entryNo?.toString() ?: "",
                        manualNo = entity.manualNo ?: "",
                        srNo = entity.srNo?.toString() ?: "",
                        entryType = entity.entryType ?: "",
                        entryDate = entity.entryDate?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        refNo = entity.refNo ?: "",
                        acId = entity.acId?.toString() ?: "",
                        dr = entity.dr.toString(),
                        cr = entity.cr.toString(),
                        narration = entity.narration ?: "",
                        isClere = if (entity.isClere) "True" else "False",
                        trascType = entity.trascType ?: "",
                        gstRate = entity.gstRate.toString(),
                        amt = entity.amt.toString(),
                        igst = entity.igst.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error mapping ledger entries")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(entryTypeFilter, dateFromSearch, dateToSearch, narrationSearch, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        // Entry Type Filter
                        val entryTypeMatch = when (entryTypeFilter) {
                            "All Types" -> true
                            else -> entry.entryType.equals(entryTypeFilter, ignoreCase = true)
                        }

                        // Date Range Filter (simplified for now)
                        val dateMatch = true // TODO: Implement date filtering

                        // Narration Search Filter
                        val narrationMatch = if (narrationSearch.isBlank()) true else
                            entry.narration.contains(narrationSearch, ignoreCase = true)

                        entryTypeMatch && dateMatch && narrationMatch
                    } catch (e: Exception) {
                        Timber.e(e, "Error filtering entry: ${entry.entryNo}")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during filtering")
            emptyList()
        }
    }

    // Calculate totals from string values
    val totalDr = remember(filteredEntries) {
        filteredEntries.sumOf { it.dr.toDoubleOrNull() ?: 0.0 }
    }
    val totalCr = remember(filteredEntries) {
        filteredEntries.sumOf { it.cr.toDoubleOrNull() ?: 0.0 }
    }
    val totalAmt = remember(filteredEntries) {
        filteredEntries.sumOf { it.amt.toDoubleOrNull() ?: 0.0 }
    }

    // Calculate totals including account opening values
    val openingDrValue = remember(openingDr) { openingDr.toDoubleOrNull() ?: 0.0 }
    val openingCrValue = remember(openingCr) { openingCr.toDoubleOrNull() ?: 0.0 }
    
    val totalDrWithOpening = remember(totalDr, openingDrValue) {
        totalDr + openingDrValue
    }
    val totalCrWithOpening = remember(totalCr, openingCrValue) {
        totalCr + openingCrValue
    }
    
    // Calculate closing balance (Total Credit - Total Debit)
    val closingBalance = remember(totalCrWithOpening, totalDrWithOpening) {
        totalCrWithOpening - totalDrWithOpening
    }

    // Unified loading screen (matches Outstanding)
    if (isScreenLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ReportLoading(
                title = "Loading Ledger Report...",
                message = loadingMessage,
                progressPercent = loadingProgress
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Header (refresh removed)
        ResponsiveReportHeader(
            title = "Ledger Report",
            subtitle = "Ledger entries and transactions",
            onBackClick = onBackClick,
            actions = {
                // Share PDF action
                IconButton(onClick = {
                    // Launch coroutine to generate and share using paginated PDF similar to Price List
                    scope.launch {
                        generateAndShareLedgerPDF(
                            context = context,
                            entries = filteredEntries,
                            totalDr = totalDrWithOpening,
                            totalCr = totalCrWithOpening,
                            closingBalance = closingBalance
                        )
                    }
                }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF", tint = JivaColors.White)
                }
            }
        )


        // Main content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Filter Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Account Name (from API)
                        val accountOptions by viewModel.accountOptions.collectAsState()
                        var accountNameSearch by remember { mutableStateOf("") }
                        var isAccountDropdownExpanded by remember { mutableStateOf(false) }
                        var selectedAccount by remember { mutableStateOf<com.example.jiva.viewmodel.LedgerReportViewModel.AccountOption?>(null) }

                        val filteredAccountOptions = remember(accountNameSearch, accountOptions) {
                            if (accountNameSearch.isBlank()) {
                                accountOptions
                            } else {
                                accountOptions.filter { it.name.contains(accountNameSearch, ignoreCase = true) }
                            }
                        }

                        Column {
                            Text(
                                text = "Account Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = isAccountDropdownExpanded,
                                onExpandedChange = { isAccountDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = accountNameSearch,
                                    onValueChange = {
                                        accountNameSearch = it
                                        isAccountDropdownExpanded = true
                                    },
                                    placeholder = { Text("Type to search account") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (filteredAccountOptions.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = isAccountDropdownExpanded,
                                        onDismissRequest = { isAccountDropdownExpanded = false }
                                    ) {
                                        filteredAccountOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.name) },
                                                onClick = {
                                                    selectedAccount = option
                                                    accountNameSearch = option.name
                                                    isAccountDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Show button (triggers Accounts -> set opening, then Ledger with filter)
                        Button(
                            onClick = {
                                val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull() ?: 1017
                                val fy = year
                                val acct = selectedAccount
                                if (acct == null) {
                                    Toast.makeText(context, "Please select an account", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        try {
                                            // 1) Fetch opening balance and CR/DR for selected account
                                            val filters = mapOf("aC_ID" to acct.id)
                                            val accRes = viewModel.fetchAccountOpening(userId, fy, filters)
                                            accRes?.let { (opening, crdr) ->
                                                if (crdr.equals("DR", true)) {
                                                    openingDr = opening
                                                    openingCr = "0.00"
                                                } else {
                                                    openingCr = opening
                                                    openingDr = "0.00"
                                                }
                                            }
                                            // 2) Fetch ledger filtered by aC_ID (server) and update local display source
                                            viewModel.loadLedgerFiltered(userId, fy, filters)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to load ledger: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Purple)
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "Show")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHOW")
                        }
                    }
                }
            }

            // Account Opening Card (CR/DR only)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account Opening",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", (openingDr.toDoubleOrNull() ?: 0.0))}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Red
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", (openingCr.toDoubleOrNull() ?: 0.0))}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Green
                                )
                            }
                        }
                    }
                }
            }

            // Data Table Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ledger Entries (${filteredEntries.size} entries)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Outstanding Report style table with horizontal scrolling
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header in Outstanding Report style
                            LedgerTableHeader()

                            // Data rows in Outstanding Report style
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredEntries) { entry ->
                                    LedgerTableRow(entry = entry)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Total Card (Total Debit, Total Credit & Closing Balance)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // First row: Total Debit and Total Credit (including opening)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Debit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalDrWithOpening)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Red
                                )
                                if (openingDrValue > 0) {
                                    Text(
                                        text = "(Opening: ₹${String.format("%.2f", openingDrValue)})",
                                        fontSize = 10.sp,
                                        color = JivaColors.DarkGray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Credit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalCrWithOpening)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Green
                                )
                                if (openingCrValue > 0) {
                                    Text(
                                        text = "(Opening: ₹${String.format("%.2f", openingCrValue)})",
                                        fontSize = 10.sp,
                                        color = JivaColors.DarkGray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                        
                        // Divider
                        HorizontalDivider(
                            color = JivaColors.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // Second row: Closing Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Closing Balance",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", kotlin.math.abs(closingBalance))}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (closingBalance >= 0) JivaColors.Green else JivaColors.Red
                                )
                                Text(
                                    text = if (closingBalance >= 0) "Credit Balance" else "Debit Balance",
                                    fontSize = 12.sp,
                                    color = if (closingBalance >= 0) JivaColors.Green else JivaColors.Red,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTableHeader() {
    Row(
        modifier = Modifier
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerHeaderCell("Entry No", Modifier.width(80.dp))
        LedgerHeaderCell("Manual No", Modifier.width(80.dp))
        LedgerHeaderCell("Type", Modifier.width(100.dp))
        LedgerHeaderCell("Date", Modifier.width(100.dp))
        LedgerHeaderCell("Ref No", Modifier.width(80.dp))
        LedgerHeaderCell("AC ID", Modifier.width(80.dp))
        LedgerHeaderCell("DR", Modifier.width(100.dp))
        LedgerHeaderCell("CR", Modifier.width(100.dp))
        LedgerHeaderCell("Narration", Modifier.width(200.dp))
    }
}

@Composable
private fun LedgerHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun LedgerTableRow(entry: LedgerEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                entryNo = entry.entryNo.takeIf { it.isNotBlank() } ?: "N/A",
                manualNo = entry.manualNo.takeIf { it.isNotBlank() } ?: "",
                entryType = entry.entryType.takeIf { it.isNotBlank() } ?: "",
                entryDate = entry.entryDate.takeIf { it.isNotBlank() } ?: "",
                refNo = entry.refNo.takeIf { it.isNotBlank() } ?: "",
                acId = entry.acId.takeIf { it.isNotBlank() } ?: "",
                dr = entry.dr.takeIf { it.isNotBlank() } ?: "0.00",
                cr = entry.cr.takeIf { it.isNotBlank() } ?: "0.00",
                narration = entry.narration.takeIf { it.isNotBlank() } ?: "",
                isClere = entry.isClere.takeIf { it.isNotBlank() } ?: "False",
                gstRate = entry.gstRate.takeIf { it.isNotBlank() } ?: "0.00",
                amt = entry.amt.takeIf { it.isNotBlank() } ?: "0.00",
                igst = entry.igst.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            Timber.e(e, "Error processing entry: ${entry.entryNo}")
            LedgerEntry("Error", "Error loading data", "", "", "", "", "", "", "", "", "", "", "", "", "")
        }
    }

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerCell(safeEntry.entryNo, Modifier.width(80.dp), JivaColors.DeepBlue)
            LedgerCell(safeEntry.manualNo, Modifier.width(80.dp))
            LedgerCell(safeEntry.entryType, Modifier.width(100.dp), JivaColors.Purple)
            LedgerCell(safeEntry.entryDate, Modifier.width(100.dp))
            LedgerCell(safeEntry.refNo, Modifier.width(80.dp))
            LedgerCell(safeEntry.acId, Modifier.width(80.dp), JivaColors.DeepBlue)
            LedgerCell("₹${safeEntry.dr}", Modifier.width(100.dp), JivaColors.Red)
            LedgerCell("₹${safeEntry.cr}", Modifier.width(100.dp), JivaColors.Green)
            LedgerCell(safeEntry.narration, Modifier.width(200.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun LedgerCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    // Safe text processing before rendering (Outstanding Report style)
    val safeText = remember(text) {
        try {
            text.takeIf { it.isNotBlank() } ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error processing text: $text")
            "Error"
        }
    }

    Text(
        text = safeText,
        fontSize = 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun LedgerTotalRow(totalDr: Double, totalCr: Double, totalEntries: Int) {
    Column {
        HorizontalDivider(
            color = JivaColors.DeepBlue,
            thickness = 2.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier
                .background(JivaColors.LightBlue.copy(alpha = 0.3f))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty cells to align with data columns (EntryNo, ManualNo, Type, Date, RefNo, AC ID)
            repeat(6) {
                Box(modifier = Modifier.width(80.dp))
            }

            // Total DR cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalDr)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total CR cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalCr)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total entries cell (Narration column width)
            Text(
                text = "$totalEntries entries",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}
