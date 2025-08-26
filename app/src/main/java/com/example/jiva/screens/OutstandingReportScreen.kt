package com.example.jiva.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.OutstandingReportViewModel
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Data model for Outstanding Report entries (all Strings to save space)
data class OutstandingEntry(
    val acId: String,
    val accountName: String,
    val mobile: String,
    val under: String,
    val balance: String,
    val lastDate: String,
    val days: String,
    val creditLimitAmount: String,
    val creditLimitDays: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: OutstandingReportViewModel = viewModel(
        factory = OutstandingReportViewModel.Factory(application.database)
    )
    
    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()
    
    // State management
    var outstandingOf by remember { mutableStateOf("Customer") }
    var viewAll by remember { mutableStateOf(false) }
    var interestRate by remember { mutableStateOf("0.06") }
    var partyNameSearch by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("All") }
    var isOutstandingDropdownExpanded by remember { mutableStateOf(false) }
    var isAreaDropdownExpanded by remember { mutableStateOf(false) }

    // Selection state for checkboxes
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }

    // Sync + observe Outstanding table
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    LaunchedEffect(userId, year) {
        if (userId != null) {
            viewModel.syncOutstanding(userId, year)
        }
    }

    val outstandingEntities by viewModel.observeOutstanding(year).collectAsState(initial = emptyList())

    // Prefer Outstanding DB data when available; fall back to legacy combined data
    val allEntries = remember(outstandingEntities, uiState.outstandingEntries) {
        if (outstandingEntities.isNotEmpty()) {
            outstandingEntities.map {
                OutstandingEntry(
                    acId = it.acId,
                    accountName = it.accountName,
                    mobile = it.mobile,
                    under = it.under,
                    balance = it.balance,
                    lastDate = it.lastDate,
                    days = it.days,
                    creditLimitAmount = it.creditLimitAmount,
                    creditLimitDays = it.creditLimitDays
                )
            }
        } else {
            // Map legacy model to new presentation with minimal info
            uiState.outstandingEntries.map {
                OutstandingEntry(
                    acId = it.acId,
                    accountName = it.accountName,
                    mobile = it.mobile,
                    under = "",
                    balance = it.balance,
                    lastDate = "",
                    days = "",
                    creditLimitAmount = "",
                    creditLimitDays = ""
                )
            }
        }
    }

    // Filtered entries based on account name or mobile only
    val filteredEntries = remember(partyNameSearch, allEntries) {
        allEntries.filter { entry ->
            if (partyNameSearch.isBlank()) true else
                entry.accountName.contains(partyNameSearch, ignoreCase = true) ||
                entry.mobile.contains(partyNameSearch, ignoreCase = true)
        }
    }

    // Handle select all functionality
    LaunchedEffect(selectAll, filteredEntries) {
        if (selectAll) {
            selectedEntries = filteredEntries.map { it.acId }.toSet()
        } else {
            selectedEntries = emptySet()
        }
    }

    // Update selectAll state based on individual selections
    LaunchedEffect(selectedEntries, filteredEntries) {
        selectAll = filteredEntries.isNotEmpty() && selectedEntries.containsAll(filteredEntries.map { it.acId })
    }

    // Calculate totals (balance stored as String; parse to Double safely)
    val totalBalance = filteredEntries.sumOf { it.balance.toDoubleOrNull() ?: 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header
        ResponsiveReportHeader(
            title = "Outstanding Report",
            subtitle = "Manage outstanding payments and dues",
            onBackClick = onBackClick
        )

        // Main content with performance optimizations
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
            // Control Panel Card
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
                        // Simplified controls: only search by Account Name or Mobile

                        // Removed Interest Calculation Section as per new requirements

                        // Search and Filter Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Party Name Search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Party Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = partyNameSearch,
                                    onValueChange = { partyNameSearch = it },
                                    placeholder = { Text("Search by name or ID...") },
                                    trailingIcon = {
                                        IconButton(onClick = { partyNameSearch = "" }) {
                                            Icon(
                                                imageVector = if (partyNameSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                                                contentDescription = if (partyNameSearch.isNotEmpty()) "Clear" else "Search"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // Area dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Area",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isAreaDropdownExpanded,
                                    onExpandedChange = { isAreaDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedArea,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAreaDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isAreaDropdownExpanded,
                                        onDismissRequest = { isAreaDropdownExpanded = false }
                                    ) {
                                        listOf("All", "North", "South", "East", "West").forEach { area ->
                                            DropdownMenuItem(
                                                text = { Text(area) },
                                                onClick = {
                                                    selectedArea = area
                                                    isAreaDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Selection info and action buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Selection summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected: ${selectedEntries.size} of ${filteredEntries.size} entries",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectAll,
                                        onCheckedChange = { selectAll = it },
                                        colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                                    )
                                    Text(
                                        text = "Select All",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue
                                    )
                                }
                            }

                            // Action button
                            Button(
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) {
                                        // TODO: Send WhatsApp to selected entries
                                        val selectedData = filteredEntries.filter { it.acId in selectedEntries }
                                        // Handle WhatsApp sending logic here
                                    }
                                },
                                enabled = selectedEntries.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Green,
                                    disabledContainerColor = JivaColors.Green.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send WhatsApp (${selectedEntries.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    // Single Share Button
                    Button(
                        onClick = {
                            scope.launch {
                                generateAndSharePDF(context, filteredEntries, totalBalance)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp green
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = JivaColors.White
                            )
                            Text(
                                text = "SHARE REPORT",
                                color = JivaColors.White,
                                fontWeight = FontWeight.Medium
                            )
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
                            text = "Outstanding Report Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Horizontally scrollable table
                        val tableScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                        ) {
                            // Table Header
                            OutstandingTableHeader()

                            // Table Rows
                            filteredEntries.forEach { entry ->
                                OutstandingTableRow(
                                    entry = entry,
                                    isSelected = selectedEntries.contains(entry.acId),
                                    onSelectionChange = { isSelected ->
                                        selectedEntries = if (isSelected) {
                                            selectedEntries + entry.acId
                                        } else {
                                            selectedEntries - entry.acId
                                        }
                                    }
                                )
                            }

                            // Total Row
                            OutstandingTotalRow(
                                totalBalance = totalBalance
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutstandingTableHeader() {
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
        // Checkbox column header
        Box(
            modifier = Modifier.width(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Select",
                tint = JivaColors.DeepBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        OutstandingHeaderCell("AC ID", Modifier.width(80.dp))
        OutstandingHeaderCell("Account Name", Modifier.width(180.dp))
        OutstandingHeaderCell("Mobile", Modifier.width(140.dp))
        OutstandingHeaderCell("Under", Modifier.width(160.dp))
        OutstandingHeaderCell("Balance", Modifier.width(120.dp))
        OutstandingHeaderCell("Last Date", Modifier.width(140.dp))
        OutstandingHeaderCell("Days", Modifier.width(80.dp))
        OutstandingHeaderCell("Credit Limit Amt", Modifier.width(140.dp))
        OutstandingHeaderCell("Credit Limit Days", Modifier.width(140.dp))
    }
}

@Composable
private fun OutstandingHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun OutstandingTableRow(
    entry: OutstandingEntry,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox column
            Box(
                modifier = Modifier.width(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple),
                    modifier = Modifier.size(20.dp)
                )
            }
            OutstandingCell(entry.acId, Modifier.width(80.dp))
            OutstandingCell(entry.accountName, Modifier.width(180.dp))
            OutstandingCell(entry.mobile, Modifier.width(140.dp))
            OutstandingCell(entry.under, Modifier.width(160.dp))
            val balanceValue = entry.balance.replace(",", "").toDoubleOrNull() ?: 0.0
            OutstandingCell(
                text = "₹${entry.balance}",
                modifier = Modifier.width(120.dp),
                color = if (balanceValue >= 0) JivaColors.Green else JivaColors.Red
            )
            OutstandingCell(entry.lastDate, Modifier.width(140.dp))
            OutstandingCell(entry.days, Modifier.width(80.dp))
            OutstandingCell(entry.creditLimitAmount, Modifier.width(140.dp))
            OutstandingCell(entry.creditLimitDays, Modifier.width(140.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun OutstandingCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun OutstandingTotalRow(
    totalBalance: Double
) {
    Row(
        modifier = Modifier
            .background(
                JivaColors.DeepBlue.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Empty space for checkbox column
        Box(modifier = Modifier.width(50.dp))

        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(560.dp) // AC ID + Account Name + Mobile + Under columns
        )
        OutstandingCell(
            text = "₹${String.format("%.0f", totalBalance)}",
            modifier = Modifier.width(120.dp),
            color = if (totalBalance >= 0) JivaColors.Green else JivaColors.Red
        )
        // Skip Last Date, Days, Credit limits in total row
    }
}

// PDF Generation Function
private suspend fun generateAndSharePDF(
    context: Context,
    entries: List<OutstandingEntry>,
    totalBalance: Double
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
            canvas.drawText("Outstanding Report", 297.5f, 50f, titlePaint)
            canvas.drawText("Generated on: $currentDate", 297.5f, 75f, cellPaint)

            // Table dimensions
            val startX = 30f
            val startY = 120f
            val rowHeight = 25f
            val colWidths = floatArrayOf(60f, 140f, 100f, 90f, 100f)
            val totalWidth = colWidths.sum()

            // Draw table headers
            val headers = arrayOf("AC ID", "Account Name", "Mobile", "Balance", "Area")
            var currentX = startX
            var currentY = startY

            // Header row background
            val headerRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
            canvas.drawRect(headerRect, Paint().apply { color = android.graphics.Color.LTGRAY; style = Paint.Style.FILL })

            // Draw header text and borders
            for (i in headers.indices) {
                val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                canvas.drawRect(rect, borderPaint)
                canvas.drawText(headers[i], currentX + 5f, currentY + 15f, headerPaint)
                currentX += colWidths[i]
            }

            // Draw data rows
            currentY += rowHeight
            for (entry in entries.take(25)) { // Limit to 25 entries to fit on page
                currentX = startX
                val rowData = arrayOf(
                    entry.acId,
                    entry.accountName.take(20), // Truncate long names
                    entry.mobile,
                    "₹${entry.balance}",
                    entry.under.take(12) // Use 'under' as area/category
                )

                for (i in rowData.indices) {
                    val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(rowData[i], currentX + 5f, currentY + 15f, cellPaint)
                    currentX += colWidths[i]
                }
                currentY += rowHeight
            }

            // Draw totals row
            currentX = startX
            val totalRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
            canvas.drawRect(totalRect, Paint().apply { color = android.graphics.Color.CYAN; style = Paint.Style.FILL; alpha = 100 })

            val totalData = arrayOf(
                "TOTAL",
                "",
                "",
                "₹${String.format("%.0f", totalBalance)}",
                ""
            )

            for (i in totalData.indices) {
                val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                canvas.drawRect(rect, borderPaint)
                canvas.drawText(totalData[i], currentX + 5f, currentY + 15f, headerPaint)
                currentX += colWidths[i]
            }

            // Add footer
            canvas.drawText("Total Entries: ${entries.size}", startX, currentY + 50f, cellPaint)
            canvas.drawText("Generated by JIVA App", startX, currentY + 70f, cellPaint)

            pdfDocument.finishPage(page)

            // Save PDF to external storage
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Outstanding_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(downloadsDir, fileName)

            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()
            pdfDocument.close()

            // Show success message and share
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                sharePDF(context, file)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun sharePDF(context: Context, file: File) {
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
            putExtra(Intent.EXTRA_SUBJECT, "Outstanding Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the Outstanding Report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Outstanding Report")
        context.startActivity(chooser)

    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun OutstandingReportScreenPreview() {
    OutstandingReportScreenImpl()
}
