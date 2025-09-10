@file:OptIn(ExperimentalMaterial3Api::class)
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
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import com.example.jiva.viewmodel.StockReportViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stock Entry data class - Complete with all API fields
 */
data class StockEntry(
    val itemId: String,
    val itemName: String,
    val opening: String,
    val inWard: String,
    val outWard: String,
    val closingStock: String,
    val avgRate: String,
    val valuation: String,
    val itemType: String,
    val company: String,
    val cgst: String,
    val sgst: String,
    val igst: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication

    // Create the ViewModel with the repository
    val viewModel: StockReportViewModel = viewModel(
        factory = StockReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }

    // Filter state: only itemType, item_Name, company
    var itemTypeFilter by remember { mutableStateOf("") }
    var itemNameSearch by remember { mutableStateOf("") }
    var companySearch by remember { mutableStateOf("") }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull() ?: 1024

    // Auto-fetch Stock API on screen load
    LaunchedEffect(Unit) {
        try {
            isScreenLoading = true
            loadingMessage = "Loading Stock data..."
            loadingProgress = 10

            val result = com.example.jiva.utils.ApiDataManager.refreshStockData(
                context = context,
                repository = application.repository,
                database = application.database,
                userId = finalUserId,
                year = year
            )

            loadingProgress = 80
            loadingMessage = if (result.isSuccess) "Processing data..." else (result.exceptionOrNull()?.message ?: "Failed to load")
        } catch (_: Exception) {
            loadingMessage = "Failed to load"
        } finally {
            loadingProgress = 100
            isScreenLoading = false
        }
    }

    // Optimized data loading - only from Room DB for better performance
    val stockEntities by viewModel.observeStock(year).collectAsState(initial = emptyList())

    // Use only Stock DB data for better performance and stability
    val allEntries = remember(stockEntities) {
        try {
            stockEntities.map { entity ->
                StockEntry(
                    itemId = entity.itemId,
                    itemName = entity.itemName,
                    opening = entity.opening,
                    inWard = entity.inWard,
                    outWard = entity.outWard,
                    closingStock = entity.closingStock,
                    avgRate = entity.avgRate,
                    valuation = entity.valuation,
                    itemType = entity.itemType,
                    company = entity.company,
                    cgst = entity.cgst,
                    sgst = entity.sgst,
                    igst = entity.igst
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping stock entities")
            emptyList()
        }
    }

    // Filtering: only itemType, item_Name, company
    val filteredEntries = remember(itemTypeFilter, itemNameSearch, companySearch, allEntries) {
        try {
            if (allEntries.isEmpty()) emptyList() else {
                allEntries.filter { entry ->
                    val typeMatch = itemTypeFilter.isBlank() || entry.itemType.contains(itemTypeFilter, ignoreCase = true)
                    val nameMatch = itemNameSearch.isBlank() || entry.itemName.contains(itemNameSearch, ignoreCase = true)
                    val companyMatch = companySearch.isBlank() || entry.company.contains(companySearch, ignoreCase = true)
                    typeMatch && nameMatch && companyMatch
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during filtering")
            emptyList()
        }
    }



    // Calculate totals from string values
    val totalOpeningStock = remember(filteredEntries) {
        filteredEntries.sumOf { it.opening.toDoubleOrNull() ?: 0.0 }
    }
    val totalInWard = remember(filteredEntries) {
        filteredEntries.sumOf { it.inWard.toDoubleOrNull() ?: 0.0 }
    }
    val totalOutWard = remember(filteredEntries) {
        filteredEntries.sumOf { it.outWard.toDoubleOrNull() ?: 0.0 }
    }
    val totalClosingStock = remember(filteredEntries) {
        filteredEntries.sumOf { it.closingStock.toDoubleOrNull() ?: 0.0 }
    }
    val totalValuation = remember(filteredEntries) {
        filteredEntries.sumOf {
            val cleanValuation = it.valuation
                .replace("₹", "")
                .replace(",", "")
                .replace(" ", "")
                .trim()
            cleanValuation.toDoubleOrNull() ?: 0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header (auto-refresh on load)
        ResponsiveReportHeader(
            title = "Stock Report",
            subtitle = "Manage stock inventory and valuation",
            onBackClick = onBackClick
        )

        // Unified loading screen (matches Outstanding)
        if (isScreenLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ReportLoading(
                    title = "Loading Stock Report...",
                    message = loadingMessage,
                    progressPercent = loadingProgress
                )
            }
        } else if (allEntries.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "No Stock Data",
                        modifier = Modifier.size(64.dp),
                        tint = JivaColors.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No stock data available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = JivaColors.DarkGray
                    )
                    Text(
                        text = "Data loads automatically on open. Try again later or check connection.",
                        fontSize = 14.sp,
                        color = JivaColors.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Main content with performance optimizations
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter Section
                item {
                    StockFilterSection(
                        itemTypeFilter = itemTypeFilter,
                        onItemTypeChange = { itemTypeFilter = it },
                        itemNameSearch = itemNameSearch,
                        onItemNameSearchChange = { itemNameSearch = it },
                        companySearch = companySearch,
                        onCompanySearchChange = { companySearch = it },
                        filteredCount = filteredEntries.size,
                        totalCount = allEntries.size
                    )
                }

                // Summary Section
                item {
                    StockSummarySection(
                        totalEntries = filteredEntries.size,
                        totalOpeningStock = totalOpeningStock,
                        totalInWard = totalInWard,
                        totalOutWard = totalOutWard,
                        totalClosingStock = totalClosingStock,
                        totalValuation = totalValuation
                    )
                }

                // Table Section
                item {
                    StockTableSection(
                        entries = filteredEntries,
                        totalValuation = totalValuation
                    )
                }

                // Share PDF Button (like Price report)
                item {
                    val tableWidth = 1302.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.width(tableWidth),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        generateAndShareStockPDF(context, filteredEntries)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Purple),
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
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Share PDF",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "SHARE PDF REPORT",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockFilterSection(
    itemTypeFilter: String,
    onItemTypeChange: (String) -> Unit,
    itemNameSearch: String,
    onItemNameSearchChange: (String) -> Unit,
    companySearch: String,
    onCompanySearchChange: (String) -> Unit,
    filteredCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filters",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue
            )

            // Item Name
            OutlinedTextField(
                value = itemNameSearch,
                onValueChange = onItemNameSearchChange,
                label = { Text("Item Name") },
                placeholder = { Text("Search by item name...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JivaColors.DeepBlue,
                    unfocusedBorderColor = JivaColors.DarkGray
                )
            )

            // Item Type
            OutlinedTextField(
                value = itemTypeFilter,
                onValueChange = onItemTypeChange,
                label = { Text("Item Type") },
                placeholder = { Text("Filter by item type...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JivaColors.DeepBlue,
                    unfocusedBorderColor = JivaColors.DarkGray
                )
            )

            // Company Name
            OutlinedTextField(
                value = companySearch,
                onValueChange = onCompanySearchChange,
                label = { Text("Company") },
                placeholder = { Text("Search by company...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JivaColors.DeepBlue,
                    unfocusedBorderColor = JivaColors.DarkGray
                )
            )

            // Filter Results Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = JivaColors.LightBlue.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Showing $filteredCount of $totalCount items",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = JivaColors.DeepBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StockSummarySection(
    totalEntries: Int,
    totalOpeningStock: Double,
    totalInWard: Double,
    totalOutWard: Double,
    totalClosingStock: Double,
    totalValuation: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Stock Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Total Items", totalEntries.toString())
                SummaryItem("Opening", String.format("%.2f", totalOpeningStock))
                SummaryItem("InWard", String.format("%.2f", totalInWard))
                SummaryItem("OutWard", String.format("%.2f", totalOutWard))
                SummaryItem("Closing", String.format("%.2f", totalClosingStock))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SummaryItem("Total Valuation", "₹${String.format("%.2f", totalValuation)}")
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = JivaColors.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StockTableSection(
    entries: List<StockEntry>,
    totalValuation: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JivaColors.DeepBlue
                )

                // Removed inline export button as requested
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Outstanding Report style table with horizontal scrolling
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header in Outstanding Report style
                StockTableHeader()

                // Data rows in Outstanding Report style
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entries) { entry ->
                        StockTableRow(entry = entry)
                    }

                    // Total row like Outstanding Report
                    item {
                        StockTotalRow(
                            totalValuation = totalValuation,
                            totalEntries = entries.size
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockTableHeader() {
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
        StockHeaderCell("Item ID", Modifier.width(80.dp))
        StockHeaderCell("Item Name", Modifier.width(150.dp))
        StockHeaderCell("Opening", Modifier.width(80.dp))
        StockHeaderCell("InWard", Modifier.width(80.dp))
        StockHeaderCell("OutWard", Modifier.width(80.dp))
        StockHeaderCell("Closing", Modifier.width(80.dp))
        StockHeaderCell("Avg Rate", Modifier.width(90.dp))
        StockHeaderCell("Valuation", Modifier.width(100.dp))
        StockHeaderCell("Type", Modifier.width(100.dp))
        StockHeaderCell("Company", Modifier.width(120.dp))
        StockHeaderCell("CGST%", Modifier.width(70.dp))
        StockHeaderCell("SGST%", Modifier.width(70.dp))
        StockHeaderCell("IGST%", Modifier.width(70.dp))
    }
}

@Composable
private fun StockHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun StockTableRow(entry: StockEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                itemId = entry.itemId.takeIf { it.isNotBlank() } ?: "N/A",
                itemName = entry.itemName.takeIf { it.isNotBlank() } ?: "Unknown",
                opening = entry.opening.takeIf { it.isNotBlank() } ?: "0",
                inWard = entry.inWard.takeIf { it.isNotBlank() } ?: "0",
                outWard = entry.outWard.takeIf { it.isNotBlank() } ?: "0",
                closingStock = entry.closingStock.takeIf { it.isNotBlank() } ?: "0",
                avgRate = entry.avgRate.takeIf { it.isNotBlank() } ?: "0",
                valuation = entry.valuation.takeIf { it.isNotBlank() } ?: "0",
                itemType = entry.itemType.takeIf { it.isNotBlank() } ?: "",
                company = entry.company.takeIf { it.isNotBlank() } ?: "",
                cgst = entry.cgst.takeIf { it.isNotBlank() } ?: "0",
                sgst = entry.sgst.takeIf { it.isNotBlank() } ?: "0",
                igst = entry.igst.takeIf { it.isNotBlank() } ?: "0"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.itemId}")
            StockEntry("Error", "Error loading data", "0", "0", "0", "0", "0", "0", "", "", "0", "0", "0")
        }
    }

    // Safe valuation parsing for color coding
    val valuationValue = remember(safeEntry.valuation) {
        try {
            safeEntry.valuation.replace(",", "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockCell(safeEntry.itemId, Modifier.width(80.dp), JivaColors.DeepBlue)
            StockCell(safeEntry.itemName, Modifier.width(150.dp), JivaColors.DarkGray)
            StockCell(safeEntry.opening, Modifier.width(80.dp))
            StockCell(safeEntry.inWard, Modifier.width(80.dp), JivaColors.Green)
            StockCell(safeEntry.outWard, Modifier.width(80.dp), JivaColors.Orange)
            StockCell(safeEntry.closingStock, Modifier.width(80.dp), JivaColors.DeepBlue)
            StockCell("₹${safeEntry.avgRate}", Modifier.width(90.dp))
            StockCell(
                text = "₹${safeEntry.valuation}",
                modifier = Modifier.width(100.dp),
                color = if (valuationValue >= 0) JivaColors.Green else JivaColors.Red
            )
            StockCell(safeEntry.itemType, Modifier.width(100.dp), JivaColors.Purple)
            StockCell(safeEntry.company, Modifier.width(120.dp))
            StockCell("${safeEntry.cgst}%", Modifier.width(70.dp))
            StockCell("${safeEntry.sgst}%", Modifier.width(70.dp))
            StockCell("${safeEntry.igst}%", Modifier.width(70.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun StockCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    // Safe text processing before rendering (Outstanding Report style)
    val safeText = remember(text) {
        try {
            text.takeIf { it.isNotBlank() } ?: ""
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing text: $text")
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
private fun StockTotalRow(totalValuation: Double, totalEntries: Int) {
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
            // "TOTAL" text aligned with Item ID column
            Text(
                text = "TOTAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )
            // Empty spaces for Item Name, Opening, InWard, OutWard, Closing, Avg Rate columns
            Box(modifier = Modifier.width(150.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(90.dp))
            
            // Total valuation cell aligned with Valuation column
            Text(
                text = "₹${String.format("%.2f", totalValuation)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total entries cell aligned with Type column
            Text(
                text = "${totalEntries} items",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Empty cells for remaining columns (Company, CGST%, SGST%, IGST%)
            Box(modifier = Modifier.width(120.dp))
            Box(modifier = Modifier.width(70.dp))
            Box(modifier = Modifier.width(70.dp))
            Box(modifier = Modifier.width(70.dp))
        }
    }
}

private suspend fun generateAndShareStockPDF(context: Context, entries: List<StockEntry>) {
    withContext(Dispatchers.IO) {
        try {
            // Landscape A4 page: 842 x 595
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
            val headerPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 8f   // further reduced to fit more columns
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 6f   // further reduced to avoid clipping
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

            val startX = margin
            val rowHeight = 18f
            val headerHeight = 24f

            // Headers for Stock data
            val headers = listOf(
                "Item ID", "Item Name", "Opening", "InWard", "OutWard",
                "Closing", "Avg Rate", "Valuation", "Type", "Company",
                "CGST%", "SGST%", "IGST%"
            )

            // Adjusted column widths to give more space to the 'Type' column
            val weights = floatArrayOf(
                0.06f, // Item ID
                0.15f, // Item Name
                0.07f, // Opening
                0.07f, // Inward
                0.07f, // Outward
                0.07f, // Closing
                0.08f, // Avg Rate
                0.09f, // Valuation
                0.11f, // Type (increased)
                0.09f, // Company
                0.06f, // CGST
                0.06f, // SGST
                0.06f  // IGST
            )
            val weightSum = weights.sum()
            val normalized = weights.map { it / weightSum }
            val colWidths = FloatArray(headers.size) { i -> (contentWidth * normalized[i]) }

            // Rows per page
            val titleBlockHeight = 30f + 20f + 15f + 25f
            val availableHeightBase = pageHeight - titleBlockHeight - headerHeight - margin - margin
            val rowsPerPage = (availableHeightBase / rowHeight).toInt().coerceAtLeast(1)

            var currentPage = 1
            var entryIndex = 0
            val totalPages = ((entries.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

            while (entryIndex < entries.size) {
                val page = pdfDocument.startPage(
                    android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                )
                val canvas = page.canvas
                var currentY = 30f

                // Title
                canvas.drawText("Stock Report", (pageWidth / 2).toFloat(), currentY, titlePaint)
                currentY += 20f
                canvas.drawText("Page $currentPage of $totalPages", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 25f

                // Table header
                var xCursor = startX
                val headerTop = currentY
                val headerBottom = currentY + headerHeight
                for (i in headers.indices) {
                    val rect = android.graphics.RectF(xCursor, headerTop, xCursor + colWidths[i], headerBottom)
                    canvas.drawRect(rect, fillHeaderPaint)
                    canvas.drawRect(rect, borderPaint)
                    drawTrimmedText(
                        canvas, headers[i],
                        xCursor + 4f, headerBottom - 8f,
                        colWidths[i] - 8f, headerPaint
                    )
                    xCursor += colWidths[i]
                }
                currentY = headerBottom

                // Rows
                val endIndex = (entryIndex + rowsPerPage).coerceAtMost(entries.size)
                for (i in entryIndex until endIndex) {
                    if (currentY + rowHeight > bottomY) break
                    val e = entries[i]
                    xCursor = startX
                    val opening = e.opening.toDoubleOrNull() ?: 0.0
                    val inWard = e.inWard.toDoubleOrNull() ?: 0.0
                    val outWard = e.outWard.toDoubleOrNull() ?: 0.0
                    val closingStock = e.closingStock.toDoubleOrNull() ?: 0.0
                    val avgRate = e.avgRate.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                    val valuation = e.valuation.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0

                    val data = listOf(
                        e.itemId,
                        e.itemName,
                        String.format("%.2f", opening),
                        String.format("%.2f", inWard),
                        String.format("%.2f", outWard),
                        String.format("%.2f", closingStock),
                        "₹${String.format("%.2f", avgRate)}",
                        "₹${String.format("%.2f", valuation)}",
                        e.itemType,
                        e.company,
                        "${e.cgst}%",
                        "${e.sgst}%",
                        "${e.igst}%"
                    )
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight
                    for (j in data.indices) {
                        val rect = android.graphics.RectF(xCursor, rowTop, xCursor + colWidths[j], rowBottom)
                        canvas.drawRect(rect, borderPaint)
                        drawTrimmedText(
                            canvas, data[j],
                            xCursor + 4f, rowBottom - 5f,
                            colWidths[j] - 8f, cellPaint
                        )
                        xCursor += colWidths[j]
                    }
                    currentY = rowBottom
                }

                pdfDocument.finishPage(page)
                entryIndex = endIndex
                currentPage++
            }

            // Save + share
            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Stock_Report_$timestamp.pdf"
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "PDF saved to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
                sharePDF(context, file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Helper function for ellipsis text drawing
private fun drawTrimmedText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: android.text.TextPaint) {
    val ellipsized = android.text.TextUtils.ellipsize(text, paint, maxWidth, android.text.TextUtils.TruncateAt.END).toString()
    canvas.drawText(ellipsized, x, y, paint)
}


// Reuse Price screen's helpers
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
    canvas.drawText(toDraw, centerX - paint.measureText(toDraw)/2, y, paint)
}

private fun sharePDF(context: Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Stock Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Stock Report attached.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Stock Report").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}