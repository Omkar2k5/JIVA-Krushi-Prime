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
import androidx.compose.material3.MenuAnchorType
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
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management
    var stockOf by remember { mutableStateOf("All Items") }
    var viewAll by remember { mutableStateOf(false) }
    var itemNameSearch by remember { mutableStateOf("") }
    var companySearch by remember { mutableStateOf("") }
    var isStockDropdownExpanded by remember { mutableStateOf(false) }

    // PDF generation state
    var isPdfGenerating by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing Stock data..."

        // Simulate progressive loading for better UX
        for (i in 0..100 step 10) {
            loadingProgress = i
            dataLoadingProgress = i.toFloat()
            loadingMessage = when {
                i < 30 -> "Loading Stock data..."
                i < 70 -> "Processing ${i}% complete..."
                i < 100 -> "Finalizing data..."
                else -> "Complete!"
            }
            kotlinx.coroutines.delay(50) // Smooth progress animation
        }

        isScreenLoading = false
    }

    // Note: Data loading is now handled automatically by AppDataLoader at app startup
    // No manual loading needed here - data is already available

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Optimized data loading - only from Room DB for better performance
    val stockEntities by viewModel.observeStock(year).collectAsState(initial = emptyList())

    // Use only Stock DB data for better performance and stability
    val allEntries = remember(stockEntities) {
        try {
            stockEntities.map { entity ->
                StockEntry(
                    itemId = entity.itemId ?: "",
                    itemName = entity.itemName ?: "",
                    opening = entity.opening ?: "0.000",
                    inWard = entity.inWard ?: "0.000",
                    outWard = entity.outWard ?: "0.000",
                    closingStock = entity.closingStock ?: "0.000",
                    avgRate = entity.avgRate ?: "0.00",
                    valuation = entity.valuation ?: "0.00",
                    itemType = entity.itemType ?: "",
                    company = entity.company ?: "",
                    cgst = entity.cgst ?: "0.00",
                    sgst = entity.sgst ?: "0.00",
                    igst = entity.igst ?: "0.00"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping stock entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(stockOf, viewAll, itemNameSearch, companySearch, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        // Stock Type Filter
                        val stockTypeMatch = when (stockOf) {
                            "All Items" -> true
                            "Pesticides" -> entry.itemType.equals("Pesticides", ignoreCase = true)
                            "Fertilizers" -> entry.itemType.equals("Fertilizers", ignoreCase = true)
                            "Seeds" -> entry.itemType.equals("Seeds", ignoreCase = true)
                            "PGR" -> entry.itemType.equals("PGR", ignoreCase = true)
                            "General" -> entry.itemType.equals("General", ignoreCase = true)
                            else -> true
                        }

                        // Stock Status Filter
                        val stockStatusMatch = if (viewAll) {
                            true
                        } else {
                            val closingStock = entry.closingStock.toDoubleOrNull() ?: 0.0
                            closingStock > 0
                        }

                        // Item Name Search Filter
                        val nameMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)

                        // Company Search Filter
                        val companyMatch = if (companySearch.isBlank()) true else
                            entry.company.contains(companySearch, ignoreCase = true)

                        stockTypeMatch && stockStatusMatch && nameMatch && companyMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry: ${entry.itemId}")
                        false
                    }
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
        // Responsive Header with Refresh Button
        ResponsiveReportHeader(
            title = "Stock Report",
            subtitle = "Manage stock inventory and valuation",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = {
                        if (finalUserId != null) {
                            isRefreshing = true
                            scope.launch {
                                try {
                                    timber.log.Timber.d("🔄 Starting Stock API refresh for userId: $finalUserId, year: $year")

                                    // Use ApiDataManager to handle API → Permanent Storage (same as Outstanding Report)
                                    val result = com.example.jiva.utils.ApiDataManager.refreshStockData(
                                        context = context,
                                        repository = application.repository,
                                        database = application.database,
                                        userId = finalUserId,
                                        year = year
                                    )

                                    if (result.isSuccess) {
                                        timber.log.Timber.d("✅ Stock data refreshed successfully")
                                        Toast.makeText(context, "Stock data refreshed successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        timber.log.Timber.e("❌ Stock data refresh failed: ${result.exceptionOrNull()?.message}")
                                        Toast.makeText(context, "Failed to refresh stock data", Toast.LENGTH_SHORT).show()
                                    }

                                    kotlinx.coroutines.delay(1000) // Show loading for at least 1 second
                                    isRefreshing = false
                                } catch (e: Exception) {
                                    timber.log.Timber.e(e, "Error during stock refresh")
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isRefreshing = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = JivaColors.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Stock Data",
                            tint = JivaColors.White
                        )
                    }
                }
            }
        )

        // High-performance loading screen with progress
        if (isScreenLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        progress = { dataLoadingProgress / 100f },
                        modifier = Modifier.size(64.dp),
                        color = JivaColors.DeepBlue,
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = loadingMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = JivaColors.DeepBlue
                    )
                    Text(
                        text = "${loadingProgress}% Complete",
                        fontSize = 10.sp,
                        color = JivaColors.DarkGray
                    )
                }
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
                        text = "Tap the refresh button to load data from server",
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
                        stockOf = stockOf,
                        onStockOfChange = { stockOf = it },
                        viewAll = viewAll,
                        onViewAllChange = { viewAll = it },
                        itemNameSearch = itemNameSearch,
                        onItemNameSearchChange = { itemNameSearch = it },
                        companySearch = companySearch,
                        onCompanySearchChange = { companySearch = it },
                        isStockDropdownExpanded = isStockDropdownExpanded,
                        onStockDropdownExpandedChange = { isStockDropdownExpanded = it },
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
                        onGeneratePDF = { data ->
                            scope.launch {
                                generateAndShareStockPDF(context, data)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StockFilterSection(
    stockOf: String,
    onStockOfChange: (String) -> Unit,
    viewAll: Boolean,
    onViewAllChange: (Boolean) -> Unit,
    itemNameSearch: String,
    onItemNameSearchChange: (String) -> Unit,
    companySearch: String,
    onCompanySearchChange: (String) -> Unit,
    isStockDropdownExpanded: Boolean,
    onStockDropdownExpandedChange: (Boolean) -> Unit,
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
                text = "Stock Filter Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue
            )

            // Stock Type Dropdown
            ExposedDropdownMenuBox(
                expanded = isStockDropdownExpanded,
                onExpandedChange = onStockDropdownExpandedChange
            ) {
                OutlinedTextField(
                    value = stockOf,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Stock Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStockDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JivaColors.DeepBlue,
                        unfocusedBorderColor = JivaColors.DarkGray
                    )
                )
                ExposedDropdownMenu(
                    expanded = isStockDropdownExpanded,
                    onDismissRequest = { onStockDropdownExpandedChange(false) }
                ) {
                    listOf("All Items", "Pesticides", "Fertilizers", "Seeds", "PGR", "General").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onStockOfChange(option)
                                onStockDropdownExpandedChange(false)
                            }
                        )
                    }
                }
            }

            // Search Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = itemNameSearch,
                    onValueChange = onItemNameSearchChange,
                    label = { Text("Item Name") },
                    placeholder = { Text("Search by item name...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JivaColors.DeepBlue,
                        unfocusedBorderColor = JivaColors.DarkGray
                    )
                )
                OutlinedTextField(
                    value = companySearch,
                    onValueChange = onCompanySearchChange,
                    label = { Text("Company") },
                    placeholder = { Text("Search by company...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JivaColors.DeepBlue,
                        unfocusedBorderColor = JivaColors.DarkGray
                    )
                )
            }

            // View All Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Show all items (including zero stock)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = JivaColors.DeepBlue
                )
                Switch(
                    checked = viewAll,
                    onCheckedChange = onViewAllChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JivaColors.White,
                        checkedTrackColor = JivaColors.Green,
                        uncheckedThumbColor = JivaColors.White,
                        uncheckedTrackColor = JivaColors.DarkGray
                    )
                )
            }

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
    onGeneratePDF: (List<StockEntry>) -> Unit
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
            // Header with PDF button
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

                Button(
                    onClick = {
                        onGeneratePDF(entries) // Generate PDF for all entries
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Green)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Generate & Share PDF"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Share PDF")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Professional table with unified horizontal scrolling
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header
                StockTableHeader()

                // Data rows in scrollable column
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(entries) { entry ->
                        StockTableRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockTableHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            HeaderCell("Item ID", 80.dp)
            HeaderCell("Item Name", 150.dp)
            HeaderCell("Opening", 80.dp)
            HeaderCell("InWard", 80.dp)
            HeaderCell("OutWard", 80.dp)
            HeaderCell("Closing", 80.dp)
            HeaderCell("Avg Rate", 90.dp)
            HeaderCell("Valuation", 100.dp)
            HeaderCell("Type", 100.dp)
            HeaderCell("Company", 120.dp)
            HeaderCell("CGST%", 70.dp)
            HeaderCell("SGST%", 70.dp)
            HeaderCell("IGST%", 70.dp)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = JivaColors.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StockTableRow(entry: StockEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataCell(entry.itemId, 80.dp, JivaColors.DeepBlue, FontWeight.Medium)
            DataCell(entry.itemName, 150.dp, JivaColors.DarkGray, FontWeight.Normal, maxLines = 2)
            DataCell(entry.opening, 80.dp, JivaColors.DarkGray)
            DataCell(entry.inWard, 80.dp, JivaColors.Green, FontWeight.Medium)
            DataCell(entry.outWard, 80.dp, JivaColors.Orange, FontWeight.Medium)
            DataCell(entry.closingStock, 80.dp, JivaColors.DeepBlue, FontWeight.Bold)
            DataCell("₹${entry.avgRate}", 90.dp, JivaColors.DarkGray)
            DataCell("₹${entry.valuation}", 100.dp, JivaColors.Green, FontWeight.Bold)
            DataCell(entry.itemType, 100.dp, JivaColors.Purple, FontWeight.Medium)
            DataCell(entry.company, 120.dp, JivaColors.DarkGray)
            DataCell("${entry.cgst}%", 70.dp, JivaColors.DarkGray)
            DataCell("${entry.sgst}%", 70.dp, JivaColors.DarkGray)
            DataCell("${entry.igst}%", 70.dp, JivaColors.DarkGray)
        }
    }
}

@Composable
private fun DataCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = JivaColors.DarkGray,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (maxLines > 1) TextAlign.Start else TextAlign.Center
        )
    }
}

// Enhanced PDF generation with auto-sharing
private suspend fun generateAndShareStockPDF(context: Context, data: List<StockEntry>) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Simple PDF content
            paint.textSize = 16f
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Stock Report", 50f, 50f, paint)

            paint.textSize = 12f
            var yPosition = 100f

            data.take(15).forEach { entry -> // Limit to 15 items for better formatting
                val line1 = "ID: ${entry.itemId} | ${entry.itemName}"
                val line2 = "Opening: ${entry.opening} | InWard: ${entry.inWard} | OutWard: ${entry.outWard} | Closing: ${entry.closingStock}"
                val line3 = "Rate: ₹${entry.avgRate} | Value: ₹${entry.valuation} | Type: ${entry.itemType} | Company: ${entry.company}"
                val line4 = "CGST: ${entry.cgst}% | SGST: ${entry.sgst}% | IGST: ${entry.igst}%"

                canvas.drawText(line1, 50f, yPosition, paint)
                yPosition += 15f
                canvas.drawText(line2, 50f, yPosition, paint)
                yPosition += 15f
                canvas.drawText(line3, 50f, yPosition, paint)
                yPosition += 15f
                canvas.drawText(line4, 50f, yPosition, paint)
                yPosition += 25f // Extra space between entries
            }

            pdfDocument.finishPage(page)

            // Save PDF
            val fileName = "Stock_Report_${java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Auto-share PDF
            withContext(Dispatchers.Main) {
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
                        putExtra(Intent.EXTRA_SUBJECT, "Stock Report - ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}")
                        putExtra(Intent.EXTRA_TEXT, "Please find attached the Stock Report generated on ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooserIntent = Intent.createChooser(shareIntent, "Share Stock Report")
                    context.startActivity(chooserIntent)

                    Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "PDF saved but sharing failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}