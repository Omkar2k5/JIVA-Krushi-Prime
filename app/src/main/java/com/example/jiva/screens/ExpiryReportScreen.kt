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
import com.example.jiva.viewmodel.ExpiryReportViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Expiry Entry data class - Complete with all API fields
 */
data class ExpiryEntry(
    val itemId: String,
    val itemName: String,
    val itemType: String,
    val batchNo: String,
    val expiryDate: String,
    val qty: String,
    val daysLeft: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: ExpiryReportViewModel = viewModel(
        factory = ExpiryReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management for filters
    var itemTypeFilter by remember { mutableStateOf("All Types") }
    var itemNameSearch by remember { mutableStateOf("") }
    var batchNoSearch by remember { mutableStateOf("") }
    var expiryStatusFilter by remember { mutableStateOf("All Items") }
    var daysLeftFilter by remember { mutableStateOf("All") }
    var isItemTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isExpiryStatusDropdownExpanded by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking and data sync
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing Expiry data..."

        // Check if we have data, if not, try to sync
        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
        if (userId != null) {
            loadingMessage = "Syncing Expiry data from server..."
            dataLoadingProgress = 25f

            try {
                val result = application.repository.syncExpiry(userId, year)
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

    // Optimized data loading - only from Room DB for better performance
    val expiryEntities by viewModel.observeExpiry(year).collectAsState(initial = emptyList())

    // Use only Expiry DB data for better performance and stability
    val allEntries = remember(expiryEntities) {
        try {
            expiryEntities.map { entity ->
                // Safely map DB -> UI and parse date string like "25/07/2025 00:00:00" to "25/07/2025"
                val formattedDate = entity.expiryDate?.let { raw ->
                    try {
                        val inFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val parsed = inFmt.parse(raw)
                        if (parsed != null) outFmt.format(parsed) else raw.substringBefore(' ')
                    } catch (_: Exception) {
                        raw.substringBefore(' ')
                    }
                } ?: ""

                ExpiryEntry(
                    itemId = entity.itemId.toString(),
                    itemName = entity.itemName,
                    itemType = entity.itemType ?: "",
                    batchNo = entity.batchNo ?: "",
                    expiryDate = formattedDate,
                    qty = try { entity.qty.toPlainString() } catch (_: Exception) { entity.qty.toString() },
                    daysLeft = entity.daysLeft?.toString() ?: "0"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping expiry entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(itemTypeFilter, itemNameSearch, batchNoSearch, expiryStatusFilter, daysLeftFilter, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        // Item Type Filter
                        val typeMatch = when (itemTypeFilter) {
                            "All Types" -> true
                            else -> entry.itemType.equals(itemTypeFilter, ignoreCase = true)
                        }
                        
                        // Item Name Search Filter
                        val nameMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)
                        
                        // Batch Number Search Filter
                        val batchMatch = if (batchNoSearch.isBlank()) true else
                            entry.batchNo.contains(batchNoSearch, ignoreCase = true)
                        
                        // Expiry Status Filter
                        val daysLeftInt = entry.daysLeft.toIntOrNull() ?: 0
                        val statusMatch = when (expiryStatusFilter) {
                            "All Items" -> true
                            "Expired" -> daysLeftInt <= 0
                            "Expiring Soon" -> daysLeftInt in 1..30
                            "Good" -> daysLeftInt > 30
                            else -> true
                        }
                        
                        // Days Left Filter
                        val daysMatch = when (daysLeftFilter) {
                            "All" -> true
                            "0-7 days" -> daysLeftInt in 0..7
                            "8-30 days" -> daysLeftInt in 8..30
                            "31-90 days" -> daysLeftInt in 31..90
                            "90+ days" -> daysLeftInt > 90
                            else -> true
                        }
                        
                        typeMatch && nameMatch && batchMatch && statusMatch && daysMatch
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

    // Calculate statistics from filtered entries
    val expiredCount = remember(filteredEntries) {
        filteredEntries.count { (it.daysLeft.toIntOrNull() ?: 0) <= 0 }
    }
    val expiringSoonCount = remember(filteredEntries) {
        filteredEntries.count { val days = it.daysLeft.toIntOrNull() ?: 0; days in 1..30 }
    }
    val totalQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.qty.toDoubleOrNull() ?: 0.0 }
    }
    // Average days left can be computed on demand in future if needed to avoid unused state
    // val avgDaysLeft = remember(filteredEntries) {
    //     if (filteredEntries.isNotEmpty()) {
    //         filteredEntries.mapNotNull { it.daysLeft.toIntOrNull() }.average()
    //     } else 0.0
    // }

    // Unified loading screen (matches Outstanding)
    if (isScreenLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ReportLoading(
                title = "Loading Expiry Report...",
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
        // Responsive Header without Refresh Button
        ResponsiveReportHeader(
            title = "Expiry Report",
            subtitle = "Item expiry tracking and alerts",
            onBackClick = onBackClick,
            actions = { }
        )

        // Main content with filters and table
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

                        // First row: Item Type and Expiry Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item Type Filter
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
                                        value = itemTypeFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isItemTypeDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isItemTypeDropdownExpanded,
                                        onDismissRequest = { isItemTypeDropdownExpanded = false }
                                    ) {
                                        listOf("All Types", "Pesticides", "Fertilizers", "Seeds", "Tools", "Others").forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    itemTypeFilter = type
                                                    isItemTypeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Expiry Status Filter
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Expiry Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isExpiryStatusDropdownExpanded,
                                    onExpandedChange = { isExpiryStatusDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = expiryStatusFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpiryStatusDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isExpiryStatusDropdownExpanded,
                                        onDismissRequest = { isExpiryStatusDropdownExpanded = false }
                                    ) {
                                        listOf("All Items", "Expired", "Expiring Soon", "Good").forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status) },
                                                onClick = {
                                                    expiryStatusFilter = status
                                                    isExpiryStatusDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Second row: Item Name and Batch Number Search
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
                                    placeholder = { Text("Search item...") },
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

                            // Batch Number Search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Search Batch Number",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = batchNoSearch,
                                    onValueChange = { batchNoSearch = it },
                                    placeholder = { Text("Search batch...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Numbers,
                                            contentDescription = "Batch",
                                            tint = JivaColors.Purple
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
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
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Expiry Statistics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Expired Items",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$expiredCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Red,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Expiring Soon",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$expiringSoonCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Orange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Total Items",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${filteredEntries.size}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.DeepBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Total Quantity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format("%.2f", totalQty),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.Green,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                text = "Expiry Items",
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

                        // Outstanding Report style table with horizontal scrolling
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header in Outstanding Report style
                            ExpiryTableHeader()

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
                                                    text = if (allEntries.isEmpty()) "No expiry data available" else "No items match your filters",
                                                    fontSize = 16.sp,
                                                    color = JivaColors.DarkGray,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredEntries) { entry ->
                                        ExpiryTableRow(entry = entry)
                                    }

                                    // Total row like Outstanding Report
                                    item {
                                        ExpiryTotalRow(
                                            expiredCount = expiredCount,
                                            expiringSoonCount = expiringSoonCount,
                                            totalQty = totalQty,
                                            totalItems = filteredEntries.size
                                        )
                                    }
                                }
                            }
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
                                    itemTypeFilter = "All Types"
                                    itemNameSearch = ""
                                    batchNoSearch = ""
                                    expiryStatusFilter = "All Items"
                                    daysLeftFilter = "All"
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
                                    generateAndSharePDF(context, filteredEntries, expiredCount, expiringSoonCount, totalQty)
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
private fun ExpiryTableHeader() {
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
        ExpiryHeaderCell("Item ID", Modifier.width(80.dp))
        ExpiryHeaderCell("Item Name", Modifier.width(180.dp))
        ExpiryHeaderCell("Type", Modifier.width(120.dp))
        ExpiryHeaderCell("Batch No", Modifier.width(100.dp))
        ExpiryHeaderCell("Expiry Date", Modifier.width(120.dp))
        ExpiryHeaderCell("Quantity", Modifier.width(100.dp))
        ExpiryHeaderCell("Days Left", Modifier.width(100.dp))
        ExpiryHeaderCell("Status", Modifier.width(100.dp))
    }
}

@Composable
private fun ExpiryHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun ExpiryTableRow(entry: ExpiryEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                itemId = entry.itemId.takeIf { it.isNotBlank() } ?: "",
                itemName = entry.itemName.takeIf { it.isNotBlank() } ?: "Unknown Item",
                itemType = entry.itemType.takeIf { it.isNotBlank() } ?: "",
                batchNo = entry.batchNo.takeIf { it.isNotBlank() } ?: "",
                expiryDate = entry.expiryDate.takeIf { it.isNotBlank() } ?: "",
                qty = entry.qty.takeIf { it.isNotBlank() } ?: "0",
                daysLeft = entry.daysLeft.takeIf { it.isNotBlank() } ?: "0"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.itemId}")
            ExpiryEntry("", "Error loading data", "", "", "", "0", "0")
        }
    }

    // Safe days left parsing for color coding
    val daysLeftInt = remember(safeEntry.daysLeft) {
        try {
            safeEntry.daysLeft.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Determine status and color
    val (status, statusColor) = remember(daysLeftInt) {
        when {
            daysLeftInt <= 0 -> "Expired" to JivaColors.Red
            daysLeftInt <= 7 -> "Critical" to JivaColors.Red
            daysLeftInt <= 30 -> "Warning" to JivaColors.Orange
            daysLeftInt <= 90 -> "Caution" to Color(0xFFFFB000)
            else -> "Good" to JivaColors.Green
        }
    }

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpiryCell(safeEntry.itemId, Modifier.width(80.dp), JivaColors.DeepBlue)
            ExpiryCell(safeEntry.itemName, Modifier.width(180.dp), JivaColors.DarkGray)
            ExpiryCell(safeEntry.itemType, Modifier.width(120.dp), JivaColors.Purple)
            ExpiryCell(safeEntry.batchNo, Modifier.width(100.dp))
            ExpiryCell(safeEntry.expiryDate, Modifier.width(120.dp))
            ExpiryCell(safeEntry.qty, Modifier.width(100.dp), JivaColors.DeepBlue)
            ExpiryCell(safeEntry.daysLeft, Modifier.width(100.dp), statusColor)
            ExpiryCell(status, Modifier.width(100.dp), statusColor)
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ExpiryCell(
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
private fun ExpiryTotalRow(expiredCount: Int, expiringSoonCount: Int, totalQty: Double, totalItems: Int) {
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
            // Empty cells to align with data columns
            repeat(4) {
                Box(modifier = Modifier.width(80.dp))
            }

            // Summary text
            Text(
                text = "$totalItems items",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Total quantity cell
            Text(
                text = "Total: ${String.format("%.2f", totalQty)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Expired count
            Text(
                text = "$expiredCount expired",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Expiring soon count
            Text(
                text = "$expiringSoonCount soon",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.Orange,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

/**
 * Generate and Share PDF for Expiry Report
 */
private suspend fun generateAndSharePDF(
    context: android.content.Context,
    entries: List<ExpiryEntry>,
    expiredCount: Int,
    expiringSoonCount: Int,
    totalQty: Double
) {
    withContext(Dispatchers.IO) {
        try {
            // Landscape A4 page: 842 x 595
            val pageWidth = 842
            val pageHeight = 595
            val margin = 30f
            val contentWidth = pageWidth - (2 * margin)
            val pdfDocument = android.graphics.pdf.PdfDocument()

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9f
                typeface = android.graphics.Typeface.DEFAULT
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }
            val fillHeaderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = android.graphics.Paint.Style.FILL
            }

            val startX = margin
            val startY = 90f
            val rowHeight = 18f

            // 8 columns to match on-screen table
            val headers = listOf("Item ID", "Item Name", "Type", "Batch No", "Expiry Date", "Quantity", "Days Left", "Status")
            val colWidths = floatArrayOf(80f, 180f, 120f, 100f, 120f, 100f, 100f, 100f)

            val page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas

            // Title
            canvas.drawText("Expiry Report", (pageWidth / 2).toFloat(), 40f, titlePaint)
            canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", (pageWidth / 2).toFloat(), 60f, cellPaint)

            // Summary section
            var currentY = startY
            canvas.drawText("Summary:", startX, currentY, headerPaint)
            currentY += 20f
            canvas.drawText("Total Items: ${entries.size}", startX, currentY, cellPaint)
            currentY += 15f
            canvas.drawText("Expired Items: $expiredCount", startX, currentY, cellPaint)
            currentY += 15f
            canvas.drawText("Expiring Soon: $expiringSoonCount", startX, currentY, cellPaint)
            currentY += 15f
            canvas.drawText("Total Quantity: ${String.format("%.2f", totalQty)}", startX, currentY, cellPaint)
            currentY += 25f

            // Table header
            var xCursor = startX
            for (i in headers.indices) {
                val rect = android.graphics.RectF(xCursor, currentY - rowHeight, xCursor + colWidths[i], currentY)
                canvas.drawRect(rect, fillHeaderPaint)
                canvas.drawRect(rect, borderPaint)
                canvas.drawText(headers[i], xCursor + 5f, currentY - 6f, headerPaint)
                xCursor += colWidths[i]
            }
            currentY += 5f

            // Data rows
            entries.take(25).forEach { entry ->
                if (currentY > pageHeight - 50f) return@forEach
                xCursor = startX
                val daysLeft = entry.daysLeft.toIntOrNull() ?: 0
                val status = when {
                    daysLeft <= 0 -> "Expired"
                    daysLeft <= 7 -> "Critical"
                    daysLeft <= 30 -> "Warning"
                    daysLeft <= 90 -> "Caution"
                    else -> "Good"
                }
                val data = listOf(
                    entry.itemId,
                    entry.itemName.take(20),
                    entry.itemType.take(12),
                    entry.batchNo.take(12),
                    entry.expiryDate,
                    entry.qty,
                    entry.daysLeft,
                    status
                )
                
                for (i in data.indices) {
                    val rect = android.graphics.RectF(xCursor, currentY - rowHeight, xCursor + colWidths[i], currentY)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(data[i], xCursor + 5f, currentY - 6f, cellPaint)
                    xCursor += colWidths[i]
                }
                currentY += rowHeight
            }

            // Total row
            if (currentY < pageHeight - 30f) {
                currentY += 10f
                xCursor = startX
                val totalRect = android.graphics.RectF(startX, currentY - rowHeight, startX + contentWidth, currentY)
                canvas.drawRect(totalRect, fillHeaderPaint)
                canvas.drawRect(totalRect, borderPaint)

                // Draw total text
                canvas.drawText("TOTAL: ${entries.size} items | Expired: $expiredCount | Expiring Soon: $expiringSoonCount", startX + 5f, currentY - 6f, headerPaint)
            }

            pdfDocument.finishPage(page)

            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Expiry_Report_$timestamp.pdf"
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
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Expiry Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Expiry Report attached.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Grant URI permission to potential receivers
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Expiry Report").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
