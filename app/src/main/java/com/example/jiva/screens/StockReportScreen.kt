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
 * Stock Entry data class - Simple and stable
 */
data class StockEntry(
    val itemId: String,
    val itemName: String,
    val openingStock: String,
    val inQty: String,
    val outQty: String,
    val closingStock: String,
    val avgRate: String,
    val valuation: String,
    val itemType: String,
    val companyName: String
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

    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()

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

    // Selection state for PDF
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

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
                    openingStock = entity.opening ?: "",
                    inQty = entity.inWard ?: "",
                    outQty = entity.outWard ?: "",
                    closingStock = entity.closingStock ?: "",
                    avgRate = entity.avgRate ?: "",
                    valuation = entity.valuation ?: "",
                    itemType = entity.itemType ?: "",
                    companyName = entity.company ?: ""
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
                            entry.companyName.contains(companySearch, ignoreCase = true)

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

    // Handle select all functionality
    LaunchedEffect(selectAll, filteredEntries) {
        if (selectAll) {
            selectedEntries = filteredEntries.map { it.itemId }.toSet()
        } else {
            selectedEntries = emptySet()
        }
    }

    // Update selectAll state based on individual selections
    LaunchedEffect(selectedEntries, filteredEntries) {
        selectAll = filteredEntries.isNotEmpty() && selectedEntries.containsAll(filteredEntries.map { it.itemId })
    }

    // Calculate totals from string values
    val totalOpeningStock = remember(filteredEntries) {
        filteredEntries.sumOf { it.openingStock.toDoubleOrNull() ?: 0.0 }
    }
    val totalInQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.inQty.toDoubleOrNull() ?: 0.0 }
    }
    val totalOutQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.outQty.toDoubleOrNull() ?: 0.0 }
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
                        progress = dataLoadingProgress / 100f,
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
                        totalInQty = totalInQty,
                        totalOutQty = totalOutQty,
                        totalClosingStock = totalClosingStock,
                        totalValuation = totalValuation
                    )
                }

                // Table Section
                item {
                    StockTableSection(
                        entries = filteredEntries,
                        selectedEntries = selectedEntries,
                        onEntrySelectionChange = { entryId, isSelected ->
                            selectedEntries = if (isSelected) {
                                selectedEntries + entryId
                            } else {
                                selectedEntries - entryId
                            }
                        },
                        selectAll = selectAll,
                        onSelectAllChange = { selectAll = it },
                        onGeneratePDF = { selectedData ->
                            scope.launch {
                                generateStockPDF(context, selectedData)
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
                        .menuAnchor(),
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
    totalInQty: Double,
    totalOutQty: Double,
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
                SummaryItem("Opening Stock", String.format("%.2f", totalOpeningStock))
                SummaryItem("Closing Stock", String.format("%.2f", totalClosingStock))
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
    selectedEntries: Set<String>,
    onEntrySelectionChange: (String, Boolean) -> Unit,
    selectAll: Boolean,
    onSelectAllChange: (Boolean) -> Unit,
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
                        val selectedData = entries.filter { selectedEntries.contains(it.itemId) }
                        onGeneratePDF(if (selectedData.isEmpty()) entries else selectedData)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Green)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Generate PDF"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Select All Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectAll,
                    onCheckedChange = onSelectAllChange,
                    colors = CheckboxDefaults.colors(checkedColor = JivaColors.DeepBlue)
                )
                Text(
                    text = "Select All (${selectedEntries.size} selected)",
                    fontSize = 14.sp,
                    color = JivaColors.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simple table
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
                item {
                    StockTableHeader()
                }

                // Data rows
                items(entries) { entry ->
                    StockTableRow(
                        entry = entry,
                        isSelected = selectedEntries.contains(entry.itemId),
                        onSelectionChange = { isSelected ->
                            onEntrySelectionChange(entry.itemId, isSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StockTableHeader() {
    Card(
        colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Select", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
            Text("Item ID", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Item Name", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
            Text("Stock", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Value", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StockTableRow(
    entry: StockEntry,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) JivaColors.LightBlue.copy(alpha = 0.3f) else JivaColors.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                modifier = Modifier.weight(0.8f),
                colors = CheckboxDefaults.colors(checkedColor = JivaColors.DeepBlue)
            )
            Text(
                text = entry.itemId,
                fontSize = 9.sp,
                color = JivaColors.DarkGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.itemName,
                fontSize = 9.sp,
                color = JivaColors.DarkGray,
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.closingStock,
                fontSize = 9.sp,
                color = JivaColors.DarkGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "₹${entry.valuation}",
                fontSize = 9.sp,
                color = JivaColors.Green,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Simple PDF generation function
private suspend fun generateStockPDF(context: Context, data: List<StockEntry>) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Simple PDF content
            paint.textSize = 16f
            paint.color = Color.BLACK
            canvas.drawText("Stock Report", 50f, 50f, paint)

            paint.textSize = 12f
            var yPosition = 100f

            data.take(20).forEach { entry -> // Limit to 20 items for simplicity
                canvas.drawText("${entry.itemId} - ${entry.itemName} - Stock: ${entry.closingStock}", 50f, yPosition, paint)
                yPosition += 20f
            }

            pdfDocument.finishPage(page)

            // Save PDF
            val fileName = "stock_report_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Show success message
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}