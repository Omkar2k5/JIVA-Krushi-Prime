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
    var isRefreshing by remember { mutableStateOf(false) }
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
    var isDaysLeftDropdownExpanded by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing Expiry data..."

        // Simulate progressive loading for better UX
        for (i in 0..100 step 10) {
            loadingProgress = i
            dataLoadingProgress = i.toFloat()
            loadingMessage = when {
                i < 30 -> "Loading Expiry data..."
                i < 70 -> "Processing ${i}% complete..."
                i < 100 -> "Finalizing data..."
                else -> "Complete!"
            }
            kotlinx.coroutines.delay(50) // Smooth progress animation
        }

        isScreenLoading = false
    }

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

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
    val avgDaysLeft = remember(filteredEntries) {
        if (filteredEntries.isNotEmpty()) {
            filteredEntries.mapNotNull { it.daysLeft.toIntOrNull() }.average()
        } else 0.0
    }

    // Show loading screen if still loading
    if (isScreenLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { dataLoadingProgress / 100f },
                    color = JivaColors.Purple,
                    modifier = Modifier.size(60.dp)
                )
                Text(
                    text = loadingMessage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = JivaColors.DeepBlue
                )
                Text(
                    text = "${loadingProgress}% Complete",
                    fontSize = 14.sp,
                    color = JivaColors.DarkGray
                )
            }
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
            title = "Expiry Report",
            subtitle = "Item expiry tracking and alerts",
            onBackClick = onBackClick,
            actions = {
                // Refresh Button
                IconButton(
                    onClick = {
                        if (finalUserId != null && !isRefreshing) {
                            scope.launch {
                                isRefreshing = true
                                try {
                                    val result = application.repository.syncExpiry(finalUserId, year)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Expiry data refreshed successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to refresh data", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        }
                    }
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
                            contentDescription = "Refresh",
                            tint = JivaColors.White
                        )
                    }
                }
            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Expired Items",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "$expiredCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Red
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Expiring Soon",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "$expiringSoonCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Orange
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Items",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "${filteredEntries.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Quantity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = String.format("%.2f", totalQty),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Green
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
                                    scope.launch {
                                        try {
                                            val pdfData = generateExpiryReportPDF(filteredEntries, expiredCount, expiringSoonCount, totalQty)
                                            sharePDF(context, pdfData, "Expiry_Report_${System.currentTimeMillis()}.pdf")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share PDF",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHARE PDF",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

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
 * Generate PDF for Expiry Report
 */
private suspend fun generateExpiryReportPDF(
    entries: List<ExpiryEntry>,
    expiredCount: Int,
    expiringSoonCount: Int,
    totalQty: Double
): ByteArray {
    return withContext(Dispatchers.IO) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint().apply { textSize = 12f }

            var y = 40f
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Expiry Report", 40f, y, paint)
            paint.isFakeBoldText = false
            paint.textSize = 10f
            y += 16f
            canvas.drawText(
                "Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                40f,
                y,
                paint
            )

            y += 20f
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("Summary:", 40f, y, paint)
            paint.isFakeBoldText = false
            y += 16f
            canvas.drawText("Total Items: ${entries.size}", 40f, y, paint)
            y += 14f
            canvas.drawText("Expired Items: $expiredCount", 40f, y, paint)
            y += 14f
            canvas.drawText("Expiring Soon: $expiringSoonCount", 40f, y, paint)
            y += 14f
            canvas.drawText("Total Quantity: ${String.format("%.2f", totalQty)}", 40f, y, paint)

            y += 20f
            paint.isFakeBoldText = true
            canvas.drawText("Detailed Expiry Data:", 40f, y, paint)
            paint.isFakeBoldText = false
            y += 16f

            // Table header
            val headers = listOf("Item ID", "Item Name", "Type", "Batch", "Expiry", "Qty", "Days", "Status")
            val colX = floatArrayOf(40f, 100f, 260f, 320f, 370f, 430f, 470f, 510f)
            headers.forEachIndexed { idx, h -> canvas.drawText(h, colX[idx], y, paint) }
            y += 12f
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 12f

            entries.take(40).forEach { entry ->
                if (y > 800f) return@forEach
                val daysLeft = entry.daysLeft.toIntOrNull() ?: 0
                val status = when {
                    daysLeft <= 0 -> "Expired"
                    daysLeft <= 7 -> "Critical"
                    daysLeft <= 30 -> "Warning"
                    daysLeft <= 90 -> "Caution"
                    else -> "Good"
                }
                canvas.drawText(entry.itemId, colX[0], y, paint)
                canvas.drawText(entry.itemName.take(20), colX[1], y, paint)
                canvas.drawText(entry.itemType.take(12), colX[2], y, paint)
                canvas.drawText(entry.batchNo, colX[3], y, paint)
                canvas.drawText(entry.expiryDate, colX[4], y, paint)
                canvas.drawText(entry.qty, colX[5], y, paint)
                canvas.drawText(entry.daysLeft, colX[6], y, paint)
                canvas.drawText(status, colX[7], y, paint)
                y += 14f
            }

            pdfDocument.finishPage(page)
            val baos = java.io.ByteArrayOutputStream()
            pdfDocument.writeTo(baos)
            pdfDocument.close()
            baos.toByteArray()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error generating PDF")
            throw e
        }
    }
}

/**
 * Share PDF file
 */
private fun sharePDF(context: android.content.Context, pdfData: ByteArray, fileName: String) {
    try {
        // Create a temporary file
        val file = java.io.File(context.cacheDir, fileName)
        file.writeBytes(pdfData)

        // Create URI for the file
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // Create share intent
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Expiry Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the attached Expiry Report.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Expiry Report"))
    } catch (e: Exception) {
        timber.log.Timber.e(e, "Error sharing PDF")
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
