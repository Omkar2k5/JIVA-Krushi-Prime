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
import com.example.jiva.viewmodel.PriceListReportViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PriceList Entry data class - Complete with all API fields
 */
data class PriceListEntry(
    val itemId: String,
    val itemName: String,
    val mrp: String,
    val creditSaleRate: String,
    val cashSaleRate: String,
    val wholesaleRate: String,
    val avgPurchaseRate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: PriceListReportViewModel = viewModel(
        factory = PriceListReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management for filters
    var itemNameSearch by remember { mutableStateOf("") }
    var sortByFilter by remember { mutableStateOf("Item Name") }
    var priceRangeFilter by remember { mutableStateOf("All Prices") }
    var rateTypeFilter by remember { mutableStateOf("All Rates") }
    var isSortByDropdownExpanded by remember { mutableStateOf(false) }
    var isPriceRangeDropdownExpanded by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking and data sync
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing PriceList data..."

        // Check if we have data, if not, try to sync
        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
        if (userId != null) {
            loadingMessage = "Syncing PriceList data from server..."
            dataLoadingProgress = 25f

            try {
                val result = application.repository.syncPriceList(userId, year)
                if (result.isSuccess) {
                    loadingMessage = "Data synced successfully"
                    dataLoadingProgress = 75f
                } else {
                    loadingMessage = "Using cached data"
                    dataLoadingProgress = 50f
                }
            } catch (e: Exception) {
                loadingMessage = "Using cached data"
                dataLoadingProgress = 50f
            }
        }

        // Simulate progressive loading for better UX
        for (i in 75..100 step 5) {
            loadingProgress = i
            dataLoadingProgress = i.toFloat()
            loadingMessage = when {
                i < 90 -> "Finalizing data..."
                else -> "Complete!"
            }
            kotlinx.coroutines.delay(50) // Smooth progress animation
        }

        isScreenLoading = false
    }

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Optimized data loading - only from Room DB for better performance
    val priceListEntities by viewModel.observePriceList(year).collectAsState(initial = emptyList())

    // Use only PriceList DB data for better performance and stability
    val allEntries = remember(priceListEntities) {
        try {
            priceListEntities.map { entity ->
                PriceListEntry(
                    itemId = entity.itemId,
                    itemName = entity.itemName,
                    mrp = entity.mrp.toString(),
                    creditSaleRate = entity.creditSaleRate.toString(),
                    cashSaleRate = entity.cashSaleRate.toString(),
                    wholesaleRate = entity.wholesaleRate.toString(),
                    avgPurchaseRate = entity.avgPurchaseRate.toString()
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping price list entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(itemNameSearch, sortByFilter, priceRangeFilter, rateTypeFilter, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                var filtered = allEntries.filter { entry ->
                    try {
                        // Item Name Search Filter
                        val nameMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)
                        
                        // Price Range Filter
                        val mrpValue = entry.mrp.toDoubleOrNull() ?: 0.0
                        val priceMatch = when (priceRangeFilter) {
                            "All Prices" -> true
                            "Under ₹100" -> mrpValue < 100
                            "₹100-₹500" -> mrpValue in 100.0..500.0
                            "₹500-₹1000" -> mrpValue in 500.0..1000.0
                            "Above ₹1000" -> mrpValue > 1000
                            else -> true
                        }
                        
                        nameMatch && priceMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry: ${entry.itemId}")
                        false
                    }
                }
                
                // Apply sorting
                when (sortByFilter) {
                    "Item Name" -> filtered.sortedBy { it.itemName }
                    "MRP (High to Low)" -> filtered.sortedByDescending { it.mrp.toDoubleOrNull() ?: 0.0 }
                    "MRP (Low to High)" -> filtered.sortedBy { it.mrp.toDoubleOrNull() ?: 0.0 }
                    "Credit Rate (High to Low)" -> filtered.sortedByDescending { it.creditSaleRate.toDoubleOrNull() ?: 0.0 }
                    "Credit Rate (Low to High)" -> filtered.sortedBy { it.creditSaleRate.toDoubleOrNull() ?: 0.0 }
                    else -> filtered
                }.sortedBy { entry ->
                    // Sort by itemId in ascending order as secondary sort
                    entry.itemId.toIntOrNull() ?: Int.MAX_VALUE
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during filtering")
            emptyList()
        }
    }

    // Calculate statistics from filtered entries
    val avgMrp = remember(filteredEntries) {
        if (filteredEntries.isNotEmpty()) {
            filteredEntries.mapNotNull { it.mrp.toDoubleOrNull() }.average()
        } else 0.0
    }
    val avgCreditRate = remember(filteredEntries) {
        if (filteredEntries.isNotEmpty()) {
            filteredEntries.mapNotNull { it.creditSaleRate.toDoubleOrNull() }.average()
        } else 0.0
    }
    val avgCashRate = remember(filteredEntries) {
        if (filteredEntries.isNotEmpty()) {
            filteredEntries.mapNotNull { it.cashSaleRate.toDoubleOrNull() }.average()
        } else 0.0
    }
    val avgPurchaseRate = remember(filteredEntries) {
        if (filteredEntries.isNotEmpty()) {
            filteredEntries.mapNotNull { it.avgPurchaseRate.toDoubleOrNull() }.average()
        } else 0.0
    }

    // Unified loading screen (matches Outstanding)
    if (isScreenLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ReportLoading(
                title = "Loading Price List Report...",
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
        // Responsive Header with Refresh Button
        ResponsiveReportHeader(
            title = "Price List",
            subtitle = "Item pricing and rate management",
            onBackClick = onBackClick,
            actions = { }
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
                            text = "Filter & Sort Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // First row: Item Name Search and Sort By
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item Name Search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Search Item Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemNameSearch,
                                    onValueChange = { itemNameSearch = it },
                                    placeholder = { Text("Search items...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = JivaColors.Purple
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            // Sort By Filter
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sort By",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isSortByDropdownExpanded,
                                    onExpandedChange = { isSortByDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = sortByFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSortByDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isSortByDropdownExpanded,
                                        onDismissRequest = { isSortByDropdownExpanded = false }
                                    ) {
                                        listOf(
                                            "Item Name",
                                            "MRP (High to Low)",
                                            "MRP (Low to High)",
                                            "Credit Rate (High to Low)",
                                            "Credit Rate (Low to High)"
                                        ).forEach { sort ->
                                            DropdownMenuItem(
                                                text = { Text(sort) },
                                                onClick = {
                                                    sortByFilter = sort
                                                    isSortByDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Second row: Price Range Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Price Range Filter
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Price Range",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isPriceRangeDropdownExpanded,
                                    onExpandedChange = { isPriceRangeDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = priceRangeFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceRangeDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isPriceRangeDropdownExpanded,
                                        onDismissRequest = { isPriceRangeDropdownExpanded = false }
                                    ) {
                                        listOf("All Prices", "Under ₹100", "₹100-₹500", "₹500-₹1000", "Above ₹1000").forEach { range ->
                                            DropdownMenuItem(
                                                text = { Text(range) },
                                                onClick = {
                                                    priceRangeFilter = range
                                                    isPriceRangeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Empty space for balance
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Summary Card
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
                            text = "Price Statistics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Avg MRP",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", avgMrp)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Green
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Avg Credit Rate",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", avgCreditRate)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.DeepBlue
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Avg Cash Rate",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "₹${String.format("%.2f", avgCashRate)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Purple
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Items",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = "${filteredEntries.size}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Orange
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Price List",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            Text(
                                text = "${filteredEntries.size} items (${allEntries.size} total)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Clear all filters
                                    itemNameSearch = ""
                                    sortByFilter = "Item Name"
                                    priceRangeFilter = "All Prices"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Orange
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Filters",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CLEAR FILTERS",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Outstanding Report style table with horizontal scrolling
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header in Outstanding Report style
                            PriceListTableHeader()

                            // Data rows in Outstanding Report style
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (filteredEntries.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "No Data",
                                                    tint = JivaColors.DarkGray,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Text(
                                                    text = if (allEntries.isEmpty()) "No price list data available" else "No items match your filters",
                                                    fontSize = 16.sp,
                                                    color = JivaColors.DarkGray,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (allEntries.isEmpty()) {
                                                    Text(
                                                        text = "Tap the refresh button to sync data",
                                                        fontSize = 14.sp,
                                                        color = JivaColors.Purple,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredEntries) { entry ->
                                        PriceListTableRow(entry = entry)
                                    }

                                    // Total row like Outstanding Report
                                    item {
                                        PriceListTotalRow(
                                            avgMrp = avgMrp,
                                            avgCreditRate = avgCreditRate,
                                            avgCashRate = avgCashRate,
                                            avgPurchaseRate = avgPurchaseRate,
                                            totalItems = filteredEntries.size
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share PDF Button
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
                                    generateAndSharePDF(context, filteredEntries, avgMrp, avgCreditRate, avgCashRate, avgPurchaseRate)
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

@Composable
private fun PriceListTableHeader() {
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
        PriceListHeaderCell("Item ID", Modifier.width(80.dp))
        PriceListHeaderCell("Item Name", Modifier.width(200.dp))
        PriceListHeaderCell("MRP", Modifier.width(100.dp))
        PriceListHeaderCell("Credit Rate", Modifier.width(120.dp))
        PriceListHeaderCell("Cash Rate", Modifier.width(120.dp))
        PriceListHeaderCell("Wholesale Rate", Modifier.width(130.dp))
        PriceListHeaderCell("Purchase Rate", Modifier.width(130.dp))
    }
}

@Composable
private fun PriceListHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun PriceListTableRow(entry: PriceListEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                itemId = entry.itemId.takeIf { it.isNotBlank() } ?: "N/A",
                itemName = entry.itemName.takeIf { it.isNotBlank() } ?: "Unknown Item",
                mrp = entry.mrp.takeIf { it.isNotBlank() } ?: "0.00",
                creditSaleRate = entry.creditSaleRate.takeIf { it.isNotBlank() } ?: "0.00",
                cashSaleRate = entry.cashSaleRate.takeIf { it.isNotBlank() } ?: "0.00",
                wholesaleRate = entry.wholesaleRate.takeIf { it.isNotBlank() } ?: "0.00",
                avgPurchaseRate = entry.avgPurchaseRate.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.itemId}")
            PriceListEntry("Error", "Error loading data", "0.00", "0.00", "0.00", "0.00", "0.00")
        }
    }

    // Safe price parsing for color coding
    val mrpValue = remember(safeEntry.mrp) {
        try {
            safeEntry.mrp.replace(",", "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    val creditRateValue = remember(safeEntry.creditSaleRate) {
        try {
            safeEntry.creditSaleRate.replace(",", "").toDoubleOrNull() ?: 0.0
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
            PriceListCell(safeEntry.itemId, Modifier.width(80.dp), JivaColors.DeepBlue)
            PriceListCell(safeEntry.itemName, Modifier.width(200.dp), JivaColors.DarkGray)
            PriceListCell(
                text = "₹${safeEntry.mrp}",
                modifier = Modifier.width(100.dp),
                color = if (mrpValue > 0) JivaColors.Green else JivaColors.DarkGray
            )
            PriceListCell(
                text = "₹${safeEntry.creditSaleRate}",
                modifier = Modifier.width(120.dp),
                color = if (creditRateValue > 0) JivaColors.DeepBlue else JivaColors.DarkGray
            )
            PriceListCell("₹${safeEntry.cashSaleRate}", Modifier.width(120.dp), JivaColors.Purple)
            PriceListCell("₹${safeEntry.wholesaleRate}", Modifier.width(130.dp), JivaColors.Orange)
            PriceListCell("₹${safeEntry.avgPurchaseRate}", Modifier.width(130.dp), JivaColors.Red)
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PriceListCell(
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
private fun PriceListTotalRow(
    avgMrp: Double,
    avgCreditRate: Double,
    avgCashRate: Double,
    avgPurchaseRate: Double,
    totalItems: Int
) {
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
            // Item ID cell
            Box(modifier = Modifier.width(80.dp))

            // Item Name cell with total text
            Text(
                text = "$totalItems items",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(200.dp)
            )

            // Average MRP cell
            Text(
                text = "Avg: ₹${String.format("%.2f", avgMrp)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Average Credit Rate cell
            Text(
                text = "Avg: ₹${String.format("%.2f", avgCreditRate)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Average Cash Rate cell
            Text(
                text = "Avg: ₹${String.format("%.2f", avgCashRate)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Purple,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Empty wholesale cell
            Box(modifier = Modifier.width(130.dp))

            // Average Purchase Rate cell
            Text(
                text = "Avg: ₹${String.format("%.2f", avgPurchaseRate)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(130.dp)
            )
        }
    }
}

/**
 * Generate and Share PDF for Price List Report with proper pagination
 */
private suspend fun generateAndSharePDF(
    context: android.content.Context,
    entries: List<PriceListEntry>,
    avgMrp: Double,
    avgCreditRate: Double,
    avgCashRate: Double,
    avgPurchaseRate: Double
) {
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

            val startX = margin
            val rowHeight = 18f
            val headerHeight = 24f

            // Calculate optimal column widths based on content
            val headers = listOf("Item ID", "Item Name", "MRP", "Credit Rate", "Cash Rate", "Wholesale Rate", "Purchase Rate")
            val colWidths = calculateOptimalColumnWidths(entries, headers, contentWidth, cellPaint)

            // Calculate how many rows fit per page (reserve top + bottom margins and header block)
            val titleBlockHeight = 30f + 20f + 15f + 25f // title + generated + page + spacing
            val summaryBlockHeight = 0f // we will add only on first page dynamically
            val availableHeightBase = pageHeight - titleBlockHeight - headerHeight - margin - margin
            val rowsPerPage = (availableHeightBase / rowHeight).toInt().coerceAtLeast(1)

            var currentPage = 1
            var entryIndex = 0
            val totalPages = ((entries.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

            // Get company name for PDF title
            val companyName = com.example.jiva.utils.UserEnv.getCompanyName(context) ?: "Price List Report"

            while (entryIndex < entries.size) {
                val page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create())
                val canvas = page.canvas

                var currentY = 30f

                // Title and page info
                canvas.drawText(companyName, (pageWidth / 2).toFloat(), currentY, titlePaint)
                currentY += 20f
                canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 15f
                canvas.drawText("Page $currentPage of $totalPages", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 25f

                // Summary section (only on first page)
                if (currentPage == 1) {
                    canvas.drawText("Summary:", startX, currentY, headerPaint)
                    currentY += 15f
                    canvas.drawText("Total Items: ${entries.size}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Average MRP: ₹${String.format("%.2f", avgMrp)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Average Credit Rate: ₹${String.format("%.2f", avgCreditRate)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Average Cash Rate: ₹${String.format("%.2f", avgCashRate)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Average Purchase Rate: ₹${String.format("%.2f", avgPurchaseRate)}", startX, currentY, cellPaint)
                    currentY += 20f
                }

                // Table header (repeated on each page)
                var xCursor = startX
                val headerTop = currentY
                val headerBottom = currentY + headerHeight
                for (i in headers.indices) {
                    val rect = android.graphics.RectF(xCursor, headerTop, xCursor + colWidths[i], headerBottom)
                    canvas.drawRect(rect, fillHeaderPaint)
                    canvas.drawRect(rect, borderPaint)
                    drawTextCentered(canvas, headers[i], xCursor + colWidths[i]/2, headerBottom - 6f, colWidths[i] - 10f, headerPaint)
                    xCursor += colWidths[i]
                }
                currentY = headerBottom

                // Data rows for this page
                val endIndex = (entryIndex + rowsPerPage).coerceAtMost(entries.size)
                for (i in entryIndex until endIndex) {
                    if (currentY + rowHeight > bottomY) break
                    
                    val entry = entries[i]
                    xCursor = startX
                    val data = listOf(
                        entry.itemId,
                        entry.itemName,
                        "₹${entry.mrp}",
                        "₹${entry.creditSaleRate}",
                        "₹${entry.cashSaleRate}",
                        "₹${entry.wholesaleRate}",
                        "₹${entry.avgPurchaseRate}"
                    )
                    
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight
                    for (j in data.indices) {
                        val rect = android.graphics.RectF(xCursor, rowTop, xCursor + colWidths[j], rowBottom)
                        canvas.drawRect(rect, borderPaint)
                        drawTextCentered(canvas, data[j], xCursor + colWidths[j]/2, rowBottom - 4f, colWidths[j] - 10f, cellPaint)
                        xCursor += colWidths[j]
                    }
                    currentY = rowBottom
                }

                // Total row on last page
                if (currentPage == totalPages && currentY + rowHeight <= bottomY) {
                    val totalTop = currentY + 8f
                    val totalBottom = totalTop + rowHeight
                    xCursor = startX
                    val totalRect = android.graphics.RectF(startX, totalTop, startX + contentWidth, totalBottom)
                    canvas.drawRect(totalRect, fillHeaderPaint)
                    canvas.drawRect(totalRect, borderPaint)
                    drawTextCentered(canvas, "TOTAL: ${entries.size} items", startX + contentWidth/2, totalBottom - 4f, contentWidth - 10f, headerPaint)
                }

                pdfDocument.finishPage(page)
                entryIndex = endIndex
                currentPage++
            }

            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "PriceList_Report_$timestamp.pdf"
            val file = java.io.File(downloadsDir, fileName)

            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
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

/**
 * Calculate optimal column widths based on content for Price List Report
 */
private fun calculateOptimalColumnWidths(
    entries: List<PriceListEntry>,
    headers: List<String>,
    totalWidth: Float,
    paint: android.graphics.Paint
): FloatArray {
    val colWidths = FloatArray(headers.size)

    // Base weights for columns to stabilize layout on A4 landscape
    val weights = floatArrayOf(
        0.10f, // Item ID
        0.25f, // Item Name
        0.12f, // MRP
        0.15f, // Credit Rate
        0.15f, // Cash Rate
        0.13f, // Wholesale Rate
        0.10f  // Purchase Rate
    )

    // Normalize weights in case of drift
    val weightSum = weights.sum()
    val normalized = weights.map { it / weightSum }

    // Compute target widths by weights
    for (i in headers.indices) {
        colWidths[i] = (totalWidth * normalized[i]).coerceAtLeast(60f)
    }

    return colWidths
}

/**
 * Draw text centered in a cell
 */
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

private fun sharePDF(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Price List Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Price List Report attached.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Grant URI permission to potential receivers
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Price List Report").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
