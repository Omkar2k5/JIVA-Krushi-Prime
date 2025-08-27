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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.StockReportViewModel
import kotlinx.coroutines.launch
import com.example.jiva.utils.PDFGenerator
import com.example.jiva.utils.LowEndDeviceOptimizer
import com.example.jiva.ui.components.MemoryEfficientStockTable
import com.example.jiva.ui.components.EmergencyStockTable

// Data model for Stock Report entries - All String for faster retrieval
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
    val companyName: String,
    val cgst: String,
    val sgst: String,
    val igst: String // Added IGST field
)

@Composable
fun StockReportScreen(onBackClick: () -> Unit = {}) {
    StockReportScreenImpl(onBackClick = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication

    // Create the ViewModel with the repository
    val viewModel: StockReportViewModel = viewModel(
        factory = StockReportViewModel.Factory(application.database)
    )

    val uiState by viewModel.uiState.collectAsState()

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // Low-end device optimization with error handling
    val optimalSettings = remember {
        try {
            LowEndDeviceOptimizer.getOptimalSettings()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error getting optimal settings")
            LowEndDeviceOptimizer.OptimalSettings(
                pageSize = 25,
                maxVisibleItems = 50,
                filterChunkSize = 15,
                enableVirtualScrolling = true,
                enableProgressiveLoading = true,
                message = "Default settings applied due to error"
            )
        }
    }
    var isEmergencyMode by remember { mutableStateOf(false) }

    // Input state management (for UI inputs)
    var stockOfInput by remember { mutableStateOf("All Items") }
    var viewAllInput by remember { mutableStateOf(false) }
    var itemCodeSearch by remember { mutableStateOf("") }
    var itemNameSearch by remember { mutableStateOf("") }
    var companySearch by remember { mutableStateOf("") }
    var isStockDropdownExpanded by remember { mutableStateOf(false) }
    var isItemTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Additional UI state variables
    var selectedItemType by remember { mutableStateOf("All Items") }

    // Applied filters state (only updated when SHOW button is clicked)
    var appliedStockOf by remember { mutableStateOf("All Items") }
    var appliedViewAll by remember { mutableStateOf(false) }
    var appliedItemCodeSearch by remember { mutableStateOf("") }
    var appliedItemNameSearch by remember { mutableStateOf("") }
    var appliedCompanySearch by remember { mutableStateOf("") }
    // Additional local states used in input fields
    var brandName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var packagingSize by remember { mutableStateOf("") }
    var exempted by remember { mutableStateOf(false) }

    // WhatsApp messaging state
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // WhatsApp template message
    val whatsappTemplate = "Hello Kurshi Prime"

    // Sync + observe Stock table
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Handle initial screen loading with progress tracking and safety
    LaunchedEffect(Unit) {
        try {
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

            // Small delay to ensure everything is ready
            kotlinx.coroutines.delay(200)
            isScreenLoading = false

        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during loading animation")
            isScreenLoading = false
        }
    }

    // Note: Data loading is now handled automatically by AppDataLoader at app startup
    // No manual loading needed here - data is already available

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Optimized data loading with error handling
    val stockEntities by viewModel.observeStock(year).collectAsState(initial = emptyList())

    // High-performance data mapping with null safety
    val allStockEntries = remember(stockEntities) {
        try {
            if (stockEntities.isEmpty()) {
                timber.log.Timber.d("No stock entities found for year: $year")
                emptyList()
            } else {
                timber.log.Timber.d("Mapping ${stockEntities.size} stock entities")
                stockEntities.mapNotNull { entity ->
                    try {
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
                            companyName = entity.company ?: "",
                            cgst = entity.cgst ?: "",
                            sgst = entity.sgst ?: "",
                            igst = entity.igst ?: ""
                        )
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error mapping entity: ${entity.itemId}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping stock entities")
            emptyList()
        }
    }

    // Item type options
    val itemTypeOptions = listOf("All", "General", "Pesticides", "Fertilizers", "PGR", "Seeds")

    // Memory-efficient filtering with pagination for low-end devices
    val filterPredicate: (StockEntry) -> Boolean = remember(appliedStockOf, appliedViewAll, appliedItemCodeSearch, appliedItemNameSearch, appliedCompanySearch) {
        { entry ->
            try {
                // Item Type Filter (using correct database field)
                val stockTypeMatch = when (appliedStockOf) {
                    "All Items" -> true
                    "Pesticides" -> entry.itemType.equals("Pesticides", ignoreCase = true)
                    "Fertilizers" -> entry.itemType.equals("Fertilizers", ignoreCase = true)
                    "Seeds" -> entry.itemType.equals("Seeds", ignoreCase = true)
                    "PGR" -> entry.itemType.equals("PGR", ignoreCase = true)
                    "General" -> entry.itemType.equals("General", ignoreCase = true)
                    else -> true
                }

                // Stock Status Filter (viewAll toggle)
                val stockStatusMatch = if (appliedViewAll) {
                    true // Show all items
                } else {
                    // Show only items with stock (closing stock > 0)
                    val closingStock = entry.closingStock.toDoubleOrNull() ?: 0.0
                    closingStock > 0
                }

                // Item Code (Item ID) Search Filter
                val itemCodeMatch = if (appliedItemCodeSearch.isBlank()) true else
                    entry.itemId.contains(appliedItemCodeSearch, ignoreCase = true)

                // Item Name Search Filter
                val nameMatch = if (appliedItemNameSearch.isBlank()) true else
                    entry.itemName.contains(appliedItemNameSearch, ignoreCase = true)

                // Company Search Filter
                val companyMatch = if (appliedCompanySearch.isBlank()) true else
                    entry.companyName.contains(appliedCompanySearch, ignoreCase = true)

                // All filters must match
                stockTypeMatch && stockStatusMatch && itemCodeMatch && nameMatch && companyMatch

            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error filtering entry: ${entry.itemId}")
                false
            }
        }
    }

    // Simple filtered data approach (more stable)
    val filteredEntries = remember(appliedStockOf, appliedViewAll, appliedItemCodeSearch, appliedItemNameSearch, appliedCompanySearch, allStockEntries) {
        try {
            if (allStockEntries.isEmpty()) {
                emptyList()
            } else {
                val filtered = allStockEntries.filter { entry ->
                    try {
                        // Apply all filters
                        val stockTypeMatch = when (appliedStockOf) {
                            "All Items" -> true
                            "Pesticides" -> entry.itemType.equals("Pesticides", ignoreCase = true)
                            "Fertilizers" -> entry.itemType.equals("Fertilizers", ignoreCase = true)
                            "Seeds" -> entry.itemType.equals("Seeds", ignoreCase = true)
                            "PGR" -> entry.itemType.equals("PGR", ignoreCase = true)
                            "General" -> entry.itemType.equals("General", ignoreCase = true)
                            else -> true
                        }

                        val stockStatusMatch = if (appliedViewAll) {
                            true
                        } else {
                            val closingStock = entry.closingStock.toDoubleOrNull() ?: 0.0
                            closingStock > 0
                        }

                        val itemCodeMatch = if (appliedItemCodeSearch.isBlank()) true else
                            entry.itemId.contains(appliedItemCodeSearch, ignoreCase = true)

                        val nameMatch = if (appliedItemNameSearch.isBlank()) true else
                            entry.itemName.contains(appliedItemNameSearch, ignoreCase = true)

                        val companyMatch = if (appliedCompanySearch.isBlank()) true else
                            entry.companyName.contains(appliedCompanySearch, ignoreCase = true)

                        stockTypeMatch && stockStatusMatch && itemCodeMatch && nameMatch && companyMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry")
                        false
                    }
                }

                // Limit results for performance on low-end devices
                if (filtered.size > 500 && optimalSettings.enableVirtualScrolling) {
                    timber.log.Timber.w("Large dataset detected, limiting to 500 items")
                    filtered.take(500)
                } else {
                    filtered
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

    // Memory monitoring and emergency mode detection
    LaunchedEffect(allStockEntries.size) {
        try {
            com.example.jiva.utils.LowEndDeviceOptimizer.optimizeForLowEndDevice()

            // Enable emergency mode for very large datasets on low-end devices
            if (allStockEntries.size > 500 && optimalSettings.enableVirtualScrolling) {
                isEmergencyMode = true
                timber.log.Timber.w("📱 Emergency mode enabled for ${allStockEntries.size} items")
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during memory optimization")
            isEmergencyMode = true
        }
    }

    // Emergency memory cleanup on memory pressure
    LaunchedEffect(filteredEntries.size) {
        try {
            if (filteredEntries.size > 1000) {
                LowEndDeviceOptimizer.emergencyMemoryCleanup()
            }
        } catch (e: OutOfMemoryError) {
            timber.log.Timber.e("🚨 OutOfMemoryError detected - enabling emergency mode")
            isEmergencyMode = true
            LowEndDeviceOptimizer.emergencyMemoryCleanup()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during emergency cleanup")
        }
    }

    // Calculate totals from filtered data
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
            subtitle = "Inventory management and stock analysis",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = {
                        if (finalUserId != null) {
                            isRefreshing = true
                            scope.launch {
                                viewModel.refreshStockData(finalUserId, year, context)
                                kotlinx.coroutines.delay(1000) // Show loading for at least 1 second
                                isRefreshing = false
                            }
                        }
                    },
                    enabled = finalUserId != null && !isRefreshing,
                    modifier = Modifier
                        .background(
                            JivaColors.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
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

        // High-performance loading screen with progress and device optimization info
        if (isScreenLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = JivaColors.DeepBlue,
                        strokeWidth = 4.dp
                    )

                    Text(
                        text = "Loading Stock Report...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = JivaColors.DeepBlue
                    )

                    if (loadingProgress > 0) {
                        LinearProgressIndicator(
                            progress = dataLoadingProgress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = JivaColors.DeepBlue,
                            trackColor = JivaColors.LightGray
                        )

                        Text(
                            text = loadingMessage,
                            fontSize = 12.sp,
                            color = JivaColors.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${loadingProgress}% Complete",
                            fontSize = 10.sp,
                            color = JivaColors.DarkGray
                        )

                        if (optimalSettings.enableVirtualScrolling) {
                            Text(
                                text = "📱 ${optimalSettings.message}",
                                fontSize = 9.sp,
                                color = JivaColors.Orange,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else if (allStockEntries.isEmpty()) {
            // Empty data fallback
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "No Data",
                    modifier = Modifier.size(64.dp),
                    tint = JivaColors.Orange
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Stock Data Available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JivaColors.DeepBlue
                )
                Text(
                    text = "No stock data found for year $year",
                    fontSize = 14.sp,
                    color = JivaColors.DarkGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isScreenLoading = true
                        // Trigger data refresh
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JivaColors.DeepBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Data")
                }
            }
        } else {
            // Main content with performance optimizations
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = true
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stock Filter Options",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            // Active filters indicator (based on applied filters)
                            val activeFilters = mutableListOf<String>()
                            if (appliedStockOf != "All Items") activeFilters.add("Type: $appliedStockOf")
                            if (!appliedViewAll) activeFilters.add("In Stock Only")
                            if (appliedItemCodeSearch.isNotBlank()) activeFilters.add("Code: $appliedItemCodeSearch")
                            if (appliedItemNameSearch.isNotBlank()) activeFilters.add("Name: $appliedItemNameSearch")
                            if (appliedCompanySearch.isNotBlank()) activeFilters.add("Company: $appliedCompanySearch")

                            if (activeFilters.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = JivaColors.Green.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${activeFilters.size} filter(s) active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.Green,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // First row: Item Code, Brand Name, Item Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemCodeSearch,
                                    onValueChange = { itemCodeSearch = it },
                                    placeholder = { Text("Search by item code...") },
                                    trailingIcon = {
                                        IconButton(onClick = { itemCodeSearch = "" }) {
                                            Icon(
                                                imageVector = if (itemCodeSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                                                contentDescription = if (itemCodeSearch.isNotEmpty()) "Clear" else "Search"
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = JivaColors.DeepBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Company",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = companySearch,
                                    onValueChange = { companySearch = it },
                                    placeholder = { Text("Enter company name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemNameSearch,
                                    onValueChange = { itemNameSearch = it },
                                    placeholder = { Text("Enter item name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Second row: Item Description, Item Type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Description",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemDescription,
                                    onValueChange = { itemDescription = it },
                                    placeholder = { Text("Enter item description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Type",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isItemTypeDropdownExpanded,
                                    onExpandedChange = { isItemTypeDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = stockOfInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isItemTypeDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isItemTypeDropdownExpanded,
                                        onDismissRequest = { isItemTypeDropdownExpanded = false }
                                    ) {
                                        itemTypeOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    stockOfInput = option
                                                    isItemTypeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // View All toggle
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
                                checked = viewAllInput,
                                onCheckedChange = { viewAllInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JivaColors.White,
                                    checkedTrackColor = JivaColors.Green,
                                    uncheckedThumbColor = JivaColors.White,
                                    uncheckedTrackColor = JivaColors.DarkGray
                                )
                            )
                        }



                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Apply filters by updating applied filter states
                                    appliedStockOf = stockOfInput
                                    appliedViewAll = viewAllInput
                                    appliedItemCodeSearch = itemCodeSearch
                                    appliedItemNameSearch = itemNameSearch
                                    appliedCompanySearch = companySearch

                                    timber.log.Timber.d("🔍 Filters applied: stockOf=$appliedStockOf, viewAll=$appliedViewAll, itemCode='$appliedItemCodeSearch', itemName='$appliedItemNameSearch', company='$appliedCompanySearch'")
                                    timber.log.Timber.d("📊 Filtered results: ${filteredEntries.size} items out of ${allStockEntries.size} total")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Show",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHOW (${filteredEntries.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Clear Filters Button
                            Button(
                                onClick = {
                                    // Reset all input fields and applied filters to default values
                                    stockOfInput = "All Items"
                                    viewAllInput = true
                                    itemCodeSearch = ""
                                    itemNameSearch = ""
                                    companySearch = ""

                                    // Also reset applied filters
                                    appliedStockOf = "All Items"
                                    appliedViewAll = true
                                    appliedItemCodeSearch = ""
                                    appliedItemNameSearch = ""
                                    appliedCompanySearch = ""

                                    timber.log.Timber.d("🧹 All filters cleared - showing all ${allStockEntries.size} items")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Orange
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CLEAR",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val columns = listOf(
                                            PDFGenerator.TableColumn("Item ID", 80f) { (it as StockEntry).itemId },
                                            PDFGenerator.TableColumn("Item Name", 150f) { (it as StockEntry).itemName },
                                            PDFGenerator.TableColumn("Opening", 80f) { "${(it as StockEntry).openingStock.toInt()}" },
                                            PDFGenerator.TableColumn("In Qty", 80f) { "${(it as StockEntry).inQty.toInt()}" },
                                            PDFGenerator.TableColumn("Out Qty", 80f) { "${(it as StockEntry).outQty.toInt()}" },
                                            PDFGenerator.TableColumn("Closing", 80f) { "${(it as StockEntry).closingStock.toInt()}" },
                                            PDFGenerator.TableColumn("Valuation", 100f) { "₹${String.format("%.2f", (it as StockEntry).valuation)}" }
                                        )

                                        val totalRow = mapOf(
                                            "Item ID" to "TOTAL",
                                            "Item Name" to "",
                                            "Opening" to "${totalOpeningStock.toInt()}",
                                            "In Qty" to "${totalInQty.toInt()}",
                                            "Out Qty" to "${totalOutQty.toInt()}",
                                            "Closing" to "${totalClosingStock.toInt()}",
                                            "Valuation" to "₹${String.format("%.2f", totalValuation)}"
                                        )

                                        val config = PDFGenerator.PDFConfig(
                                            title = "Stock Report",
                                            fileName = "Stock_Report",
                                            columns = columns,
                                            data = filteredEntries,
                                            totalRow = totalRow
                                        )

                                        PDFGenerator.generateAndSharePDF(context, config)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366) // WhatsApp green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHARE",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = JivaColors.Teal
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stock Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Showing: ${filteredEntries.size} of ${allStockEntries.size} items",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.White
                                )
                                if (filteredEntries.size < allStockEntries.size) {
                                    Text(
                                        text = "Filters applied",
                                        fontSize = 10.sp,
                                        color = JivaColors.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Text(
                                text = "Total Valuation: ₹${String.format("%.2f", totalValuation)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.White
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
                            text = "Stock Report Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Horizontally scrollable table
                        val tableScrollState = rememberScrollState()

                        // Simple table for stability
                        SimpleStockTable(
                            entries = if (isEmergencyMode) filteredEntries.take(20) else filteredEntries.take(200),
                            isEmergencyMode = isEmergencyMode,
                            totalEntries = filteredEntries.size
                        )
                    }
                }
            }
        }
        } // Close the else block for loading screen
    }
}

/**
 * Simple Stock Table - Stable and crash-resistant
 */
@Composable
private fun SimpleStockTable(
    entries: List<StockEntry>,
    isEmergencyMode: Boolean,
    totalEntries: Int
) {
    Column {
        // Summary card
        if (isEmergencyMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JivaColors.Orange.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "⚠️ Emergency mode: Showing ${entries.size} of $totalEntries items",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = JivaColors.Orange,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else if (entries.size < totalEntries) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JivaColors.Blue.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "📊 Showing ${entries.size} of $totalEntries items for performance",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = JivaColors.DeepBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Simple table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JivaColors.White)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                // Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item ID", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Item Name", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("Stock", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Value", color = JivaColors.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Data rows
                items(entries) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
        StockHeaderCell("Item ID", modifier = Modifier.width(80.dp))
        StockHeaderCell("Item Name", modifier = Modifier.width(150.dp))
        StockHeaderCell("Opening", modifier = Modifier.width(80.dp))
        StockHeaderCell("IN Qty", modifier = Modifier.width(80.dp))
        StockHeaderCell("Out Qty", modifier = Modifier.width(80.dp))
        StockHeaderCell("Closing", modifier = Modifier.width(80.dp))
        StockHeaderCell("Avg Rate", modifier = Modifier.width(80.dp))
        StockHeaderCell("Valuation", modifier = Modifier.width(100.dp))
        StockHeaderCell("Type", modifier = Modifier.width(80.dp))
        StockHeaderCell("Company", modifier = Modifier.width(100.dp))
        StockHeaderCell("CGST", modifier = Modifier.width(60.dp))
        StockHeaderCell("SGST", modifier = Modifier.width(60.dp))
        StockHeaderCell("IGST", modifier = Modifier.width(60.dp))
    }
}

@Composable
private fun StockHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
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
    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockCell(entry.itemId, Modifier.width(80.dp))
            StockCell(entry.itemName, Modifier.width(150.dp))
            StockCell(entry.openingStock, Modifier.width(80.dp))
            StockCell(entry.inQty, Modifier.width(80.dp))
            StockCell(entry.outQty, Modifier.width(80.dp))
            StockCell(entry.closingStock, Modifier.width(80.dp))
            StockCell("₹${entry.avgRate}", Modifier.width(80.dp))
            StockCell("₹${entry.valuation}", Modifier.width(100.dp))
            StockCell(entry.itemType, Modifier.width(80.dp))
            StockCell(entry.companyName, Modifier.width(100.dp))
            StockCell("${entry.cgst}%", Modifier.width(60.dp))
            StockCell("${entry.sgst}%", Modifier.width(60.dp))
            StockCell("${entry.igst}%", Modifier.width(60.dp))
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
    Text(
        text = text,
        fontSize = 10.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun StockTotalRow(
    totalOpeningStock: Double,
    totalInQty: Double,
    totalOutQty: Double,
    totalClosingStock: Double,
    totalValuation: Double
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
        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(230.dp) // Item ID + Item Name columns
        )
        StockCell("${totalOpeningStock.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalInQty.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalOutQty.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalClosingStock.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("-", Modifier.width(80.dp), JivaColors.DeepBlue) // Avg Rate column
        StockCell(
            text = "₹${String.format("%.2f", totalValuation)}",
            modifier = Modifier.width(100.dp),
            color = JivaColors.Green
        )
        StockCell("-", Modifier.width(80.dp), JivaColors.DeepBlue) // Type column
        StockCell("-", Modifier.width(100.dp), JivaColors.DeepBlue) // Company column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // CGST column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // SGST column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // IGST column
    }
}

// Shimmer effect for loading animation
fun Modifier.stockShimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
private fun StockLoadingRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .stockShimmerEffect(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Data placeholders for stock columns
        repeat(13) { index ->
            val width = when (index) {
                0 -> 80.dp   // Item ID
                1 -> 150.dp  // Item Name
                2 -> 80.dp   // Opening
                3 -> 80.dp   // IN Qty
                4 -> 80.dp   // Out Qty
                5 -> 80.dp   // Closing
                6 -> 80.dp   // Avg Rate
                7 -> 100.dp  // Valuation
                8 -> 80.dp   // Type
                9 -> 100.dp  // Company
                10 -> 60.dp  // CGST
                11 -> 60.dp  // SGST
                12 -> 60.dp  // IGST
                else -> 80.dp
            }
            Box(
                modifier = Modifier
                    .width(width)
                    .height(16.dp)
                    .background(JivaColors.LightGray, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun StockReportScreenPreview() {
    StockReportScreenImpl()
}
