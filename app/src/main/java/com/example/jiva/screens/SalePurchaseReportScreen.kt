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
import com.example.jiva.viewmodel.SalePurchaseReportViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sale/Purchase Entry data class - Complete with all API fields
 */
data class SalePurchaseEntry(
    val trDate: String,
    val partyName: String,
    val gstin: String,
    val trType: String,
    val refNo: String,
    val itemName: String,
    val hsn: String,
    val category: String,
    val qty: String,
    val unit: String,
    val rate: String,
    val amount: String,
    val discount: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalePurchaseReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: SalePurchaseReportViewModel = viewModel(
        factory = SalePurchaseReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management for filters
    var transactionTypeFilter by remember { mutableStateOf("All Types") }
    var partyNameSearch by remember { mutableStateOf("") }
    var itemNameSearch by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All Categories") }
    var dateFromSearch by remember { mutableStateOf("") }
    var dateToSearch by remember { mutableStateOf("") }
    var isTransactionTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking and data sync
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing Sale/Purchase data..."

        // Check if we have data, if not, try to sync
        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
        if (userId != null) {
            loadingMessage = "Syncing Sale/Purchase data from server..."
            dataLoadingProgress = 25f

            try {
                val result = application.repository.syncSalePurchase(userId, year)
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
    val salePurchaseEntities by viewModel.observeSalePurchase(year).collectAsState(initial = emptyList())

    // Use only SalePurchase DB data for better performance and stability
    val allEntries = remember(salePurchaseEntities) {
        try {
            salePurchaseEntities.map { entity ->
                SalePurchaseEntry(
                    trDate = entity.trDate?.let { 
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) 
                    } ?: "",
                    partyName = entity.partyName ?: "",
                    gstin = entity.gstin ?: "",
                    trType = entity.trType ?: "",
                    refNo = entity.refNo ?: "",
                    itemName = entity.itemName ?: "",
                    hsn = entity.hsn ?: "",
                    category = entity.category ?: "",
                    qty = entity.qty?.toString() ?: "0",
                    unit = entity.unit ?: "",
                    rate = entity.rate?.toString() ?: "0.00",
                    amount = entity.amount?.toString() ?: "0.00",
                    discount = entity.discount?.toString() ?: "0.00"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping sale/purchase entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(transactionTypeFilter, partyNameSearch, itemNameSearch, categoryFilter, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        // Transaction Type Filter
                        val typeMatch = when (transactionTypeFilter) {
                            "All Types" -> true
                            else -> entry.trType.equals(transactionTypeFilter, ignoreCase = true)
                        }
                        
                        // Party Name Search Filter
                        val partyMatch = if (partyNameSearch.isBlank()) true else
                            entry.partyName.contains(partyNameSearch, ignoreCase = true)
                        
                        // Item Name Search Filter
                        val itemMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)
                        
                        // Category Filter
                        val categoryMatch = when (categoryFilter) {
                            "All Categories" -> true
                            else -> entry.category.equals(categoryFilter, ignoreCase = true)
                        }
                        
                        typeMatch && partyMatch && itemMatch && categoryMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry: ${entry.refNo}")
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
    val totalAmount = remember(filteredEntries) {
        filteredEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    val totalQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.qty.toDoubleOrNull() ?: 0.0 }
    }
    val totalDiscount = remember(filteredEntries) {
        filteredEntries.sumOf { it.discount.toDoubleOrNull() ?: 0.0 }
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
            title = "Sale/Purchase Report",
            subtitle = "Transaction history and item details",
            onBackClick = onBackClick,
            actions = {
                // Refresh Button
                IconButton(
                    onClick = {
                        if (finalUserId != null && !isRefreshing) {
                            scope.launch {
                                isRefreshing = true
                                try {
                                    val result = application.repository.syncSalePurchase(finalUserId, year)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "✅ Sale/Purchase data refreshed successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "❌ Failed to refresh: $error", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "❌ Network error: ${e.message}", Toast.LENGTH_LONG).show()
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

                        // First row: Transaction Type and Category
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Transaction Type Filter
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Transaction Type",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isTransactionTypeDropdownExpanded,
                                    onExpandedChange = { isTransactionTypeDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = transactionTypeFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTransactionTypeDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isTransactionTypeDropdownExpanded,
                                        onDismissRequest = { isTransactionTypeDropdownExpanded = false }
                                    ) {
                                        listOf("All Types", "Cash Sale", "Credit Sale", "Cash Purchase", "Credit Purchase").forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    transactionTypeFilter = type
                                                    isTransactionTypeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Category Filter
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Category",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isCategoryDropdownExpanded,
                                    onExpandedChange = { isCategoryDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = categoryFilter,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isCategoryDropdownExpanded,
                                        onDismissRequest = { isCategoryDropdownExpanded = false }
                                    ) {
                                        listOf("All Categories", "Pesticides", "Fertilizers", "Seeds", "Tools", "Others").forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category) },
                                                onClick = {
                                                    categoryFilter = category
                                                    isCategoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Second row: Party Name and Item Name Search
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Party Name Search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Search Party Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = partyNameSearch,
                                    onValueChange = { partyNameSearch = it },
                                    placeholder = { Text("Search party...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Party",
                                            tint = JivaColors.Purple
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

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
                                text = "Total Amount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Green
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
                                color = JivaColors.DeepBlue
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Transactions",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "${filteredEntries.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Purple
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Discount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalDiscount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Orange
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
                                text = "Sale/Purchase Transactions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            Text(
                                text = "${filteredEntries.size} entries (${allEntries.size} total)",
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
                            SalePurchaseTableHeader()

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
                                                    text = if (allEntries.isEmpty()) "No sale/purchase data available" else "No entries match your filters",
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
                                        SalePurchaseTableRow(entry = entry)
                                    }

                                    // Total row like Outstanding Report
                                    item {
                                        SalePurchaseTotalRow(
                                            totalAmount = totalAmount,
                                            totalQty = totalQty,
                                            totalDiscount = totalDiscount,
                                            totalEntries = filteredEntries.size
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
}

@Composable
private fun SalePurchaseTableHeader() {
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
        SalePurchaseHeaderCell("Date", Modifier.width(100.dp))
        SalePurchaseHeaderCell("Party", Modifier.width(150.dp))
        SalePurchaseHeaderCell("Type", Modifier.width(100.dp))
        SalePurchaseHeaderCell("Ref No", Modifier.width(80.dp))
        SalePurchaseHeaderCell("Item", Modifier.width(180.dp))
        SalePurchaseHeaderCell("HSN", Modifier.width(80.dp))
        SalePurchaseHeaderCell("Category", Modifier.width(120.dp))
        SalePurchaseHeaderCell("Qty", Modifier.width(80.dp))
        SalePurchaseHeaderCell("Unit", Modifier.width(80.dp))
        SalePurchaseHeaderCell("Rate", Modifier.width(100.dp))
        SalePurchaseHeaderCell("Amount", Modifier.width(120.dp))
        SalePurchaseHeaderCell("Discount", Modifier.width(100.dp))
        SalePurchaseHeaderCell("GSTIN", Modifier.width(150.dp))
    }
}

@Composable
private fun SalePurchaseHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun SalePurchaseTableRow(entry: SalePurchaseEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                trDate = entry.trDate.takeIf { it.isNotBlank() } ?: "",
                partyName = entry.partyName.takeIf { it.isNotBlank() } ?: "Unknown",
                gstin = entry.gstin.takeIf { it.isNotBlank() } ?: "",
                trType = entry.trType.takeIf { it.isNotBlank() } ?: "",
                refNo = entry.refNo.takeIf { it.isNotBlank() } ?: "",
                itemName = entry.itemName.takeIf { it.isNotBlank() } ?: "Unknown Item",
                hsn = entry.hsn.takeIf { it.isNotBlank() } ?: "",
                category = entry.category.takeIf { it.isNotBlank() } ?: "",
                qty = entry.qty.takeIf { it.isNotBlank() } ?: "0",
                unit = entry.unit.takeIf { it.isNotBlank() } ?: "",
                rate = entry.rate.takeIf { it.isNotBlank() } ?: "0.00",
                amount = entry.amount.takeIf { it.isNotBlank() } ?: "0.00",
                discount = entry.discount.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.refNo}")
            SalePurchaseEntry("", "Error loading data", "", "", "", "", "", "", "0", "", "0.00", "0.00", "0.00")
        }
    }

    // Safe amount parsing for color coding
    val amountValue = remember(safeEntry.amount) {
        try {
            safeEntry.amount.replace(",", "").toDoubleOrNull() ?: 0.0
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
            SalePurchaseCell(safeEntry.trDate, Modifier.width(100.dp))
            SalePurchaseCell(safeEntry.partyName, Modifier.width(150.dp), JivaColors.DeepBlue)
            SalePurchaseCell(safeEntry.trType, Modifier.width(100.dp), JivaColors.Purple)
            SalePurchaseCell(safeEntry.refNo, Modifier.width(80.dp))
            SalePurchaseCell(safeEntry.itemName, Modifier.width(180.dp), JivaColors.DarkGray)
            SalePurchaseCell(safeEntry.hsn, Modifier.width(80.dp))
            SalePurchaseCell(safeEntry.category, Modifier.width(120.dp), JivaColors.Orange)
            SalePurchaseCell(safeEntry.qty, Modifier.width(80.dp), JivaColors.DeepBlue)
            SalePurchaseCell(safeEntry.unit, Modifier.width(80.dp))
            SalePurchaseCell("₹${safeEntry.rate}", Modifier.width(100.dp))
            SalePurchaseCell(
                text = "₹${safeEntry.amount}",
                modifier = Modifier.width(120.dp),
                color = if (amountValue >= 0) JivaColors.Green else JivaColors.Red
            )
            SalePurchaseCell("₹${safeEntry.discount}", Modifier.width(100.dp), JivaColors.Orange)
            SalePurchaseCell(safeEntry.gstin, Modifier.width(150.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SalePurchaseCell(
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
private fun SalePurchaseTotalRow(totalAmount: Double, totalQty: Double, totalDiscount: Double, totalEntries: Int) {
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
            repeat(7) {
                Box(modifier = Modifier.width(80.dp))
            }

            // Total quantity cell
            Text(
                text = "Total: ${String.format("%.2f", totalQty)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )

            // Empty unit cell
            Box(modifier = Modifier.width(80.dp))

            // Empty rate cell
            Box(modifier = Modifier.width(100.dp))

            // Total amount cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalAmount)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Total discount cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalDiscount)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Orange,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total entries cell
            Text(
                text = "$totalEntries entries",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}
