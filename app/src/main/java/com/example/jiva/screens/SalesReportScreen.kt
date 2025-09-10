package com.example.jiva.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sale/Purchase Entry data class - Complete with all API fields
 */
data class SalesReportEntry(
    val trDate: String,
    val partyName: String,
    val gstin: String,
    val entryType: String,
    val refNo: String,
    val itemName: String,
    val hsnNo: String,
    val itemType: String,
    val qty: String,
    val unit: String,
    val rate: String,
    val amount: String,
    val discount: String,
    val cgst: String = "",
    val sgst: String = "",
    val igst: String = "",
    val total: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(onBackClick: () -> Unit = {}) {
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
    var selectedTransactionType by remember { mutableStateOf("Sale") } // Default to Sale
    var shouldApplyFilter by remember { mutableStateOf(true) } // Apply filter immediately, show Sales by default
    var partyNameSearch by remember { mutableStateOf("") }
    var itemNameSearch by remember { mutableStateOf("") }

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
    val allSalesEntries = remember(salePurchaseEntities) {
        try {
            salePurchaseEntities.map { entity ->
                SalesReportEntry(
                    trDate = entity.trDate?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    partyName = entity.partyName ?: "",
                    gstin = entity.gstin ?: "",
                    entryType = entity.trType ?: "",
                    refNo = entity.refNo ?: "",
                    itemName = entity.itemName ?: "",
                    hsnNo = entity.hsn ?: "",
                    itemType = entity.category ?: "",
                    qty = entity.qty?.toString() ?: "0",
                    unit = entity.unit ?: "",
                    rate = entity.rate?.toString() ?: "0.00",
                    amount = entity.amount?.toString() ?: "0.00",
                    discount = entity.discount?.toString() ?: "0.00",
                    cgst = entity.cgst,
                    sgst = entity.sgst,
                    igst = entity.igst,
                    total = entity.total ?: "0.00"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping sale/purchase entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(selectedTransactionType, shouldApplyFilter, partyNameSearch, itemNameSearch, allSalesEntries) {
        try {
            if (allSalesEntries.isEmpty() || !shouldApplyFilter) {
                emptyList()
            } else {
                allSalesEntries.filter { entry ->
                    try {
                        // Transaction Type Filter - Sale or Purchase
                        val typeMatch = when (selectedTransactionType) {
                            "Sale" -> {
                                entry.entryType.equals("Cash Sale", ignoreCase = true) ||
                                entry.entryType.equals("Credit Sale", ignoreCase = true) ||
                                entry.entryType.equals("Wholesale", ignoreCase = true) ||
                                entry.entryType.equals("Pesticide Sale", ignoreCase = true)
                            }
                            "Purchase" -> {
                                entry.entryType.equals("Cash Purchase", ignoreCase = true) ||
                                entry.entryType.equals("Credit Purchase", ignoreCase = true)
                            }
                            else -> true
                        }

                        // Party Name Search Filter
                        val partyMatch = if (partyNameSearch.isBlank()) true else
                            entry.partyName.contains(partyNameSearch, ignoreCase = true)

                        // Item Name Search Filter
                        val itemMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)

                        typeMatch && partyMatch && itemMatch
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

    // Calculate statistics from filtered entries
    val totalAmount = remember(filteredEntries) {
        filteredEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    val totalQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.qty.toDoubleOrNull() ?: 0.0 }
    }
    val totalDiscount = remember(filteredEntries) {
        filteredEntries.sumOf { it.discount.toDoubleOrNull() ?: 0.0 }
    }
    val totalSum = remember(filteredEntries) {
        filteredEntries.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
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
        // Responsive Header
        ResponsiveReportHeader(
            title = "${selectedTransactionType} Report",
            subtitle = "Transaction history and ${selectedTransactionType.lowercase()} data",
            onBackClick = onBackClick
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

                        // Transaction Type Radio Buttons
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Transaction Type",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Sale Radio Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = selectedTransactionType == "Sale",
                                        onClick = { 
                                            selectedTransactionType = "Sale"
                                            shouldApplyFilter = false // Reset filter application
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = JivaColors.Purple,
                                            unselectedColor = JivaColors.DarkGray
                                        )
                                    )
                                    Text(
                                        text = "Sale",
                                        fontSize = 14.sp,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                
                                // Purchase Radio Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = selectedTransactionType == "Purchase",
                                        onClick = { 
                                            selectedTransactionType = "Purchase"
                                            shouldApplyFilter = false // Reset filter application
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = JivaColors.Purple,
                                            unselectedColor = JivaColors.DarkGray
                                        )
                                    )
                                    Text(
                                        text = "Purchase",
                                        fontSize = 14.sp,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        // Show Button
                        Button(
                            onClick = {
                                shouldApplyFilter = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JivaColors.Purple
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SHOW",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.White
                            )
                        }

                        // Party Name Search - moved down
                        Column(modifier = Modifier.fillMaxWidth()) {
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

                        // Item Name Search - Second line
                        Column(modifier = Modifier.fillMaxWidth()) {
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

            // Responsive Summary Card
            item {
                ResponsiveSummaryCard(
                    totalAmount = totalAmount,
                    totalQty = totalQty,
                    transactionCount = filteredEntries.size,
                    totalDiscount = totalDiscount,
                    totalSum = totalSum
                )
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
                                text = "${selectedTransactionType} Transactions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            Text(
                                text = "${filteredEntries.size} entries (${allSalesEntries.size} total)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Responsive table with horizontal scrolling
                        ResponsiveSalesTable(
                            entries = filteredEntries,
                            allEntries = allSalesEntries,
                            totalAmount = totalAmount,
                            totalQty = totalQty,
                            totalDiscount = totalDiscount,
                            totalSum = totalSum
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Responsive Action Button
                        ResponsiveActionButton(
                            onClick = {
                                scope.launch {
                                    generateAndShareSalesPDF(context, filteredEntries, totalAmount, totalQty, totalDiscount, totalSum)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsiveSummaryCard(
    totalAmount: Double,
    totalQty: Double,
    transactionCount: Int,
    totalDiscount: Double,
    totalSum: Double
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp
    
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
                text = "Summary Statistics",
                fontSize = if (isCompact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue
            )
            
            if (isCompact) {
                // Vertical layout for small screens
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem("Total Amount", "₹${String.format("%.2f", totalAmount)}", JivaColors.Green)
                    SummaryItem("Total Quantity", String.format("%.2f", totalQty), JivaColors.DeepBlue)
                    SummaryItem("Transactions", "$transactionCount", JivaColors.Purple)
                    SummaryItem("Total Discount", "₹${String.format("%.2f", totalDiscount)}", JivaColors.Orange)
                    SummaryItem("Grand Total", "₹${String.format("%.2f", totalSum)}", JivaColors.Purple)
                }
            } else {
                // Grid layout for larger screens
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (screenWidth > 900.dp) 5 else 2),
                    modifier = Modifier.height(if (screenWidth > 900.dp) 80.dp else 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { SummaryItem("Total Amount", "₹${String.format("%.2f", totalAmount)}", JivaColors.Green) }
                    item { SummaryItem("Total Quantity", String.format("%.2f", totalQty), JivaColors.DeepBlue) }
                    item { SummaryItem("Transactions", "$transactionCount", JivaColors.Purple) }
                    item { SummaryItem("Total Discount", "₹${String.format("%.2f", totalDiscount)}", JivaColors.Orange) }
                    item { SummaryItem("Grand Total", "₹${String.format("%.2f", totalSum)}", JivaColors.Purple) }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            color = JivaColors.DarkGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = if (isCompact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResponsiveSalesTable(
    entries: List<SalesReportEntry>,
    allEntries: List<SalesReportEntry>,
    totalAmount: Double,
    totalQty: Double,
    totalDiscount: Double,
    totalSum: Double
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp
    
    Column(
        modifier = Modifier
            .height(if (isCompact) 350.dp else 400.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        // Header
        SalesTableHeader()

        // Data rows
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            if (entries.isEmpty()) {
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
                                modifier = Modifier.size(if (isCompact) 40.dp else 48.dp)
                            )
                            Text(
                                text = if (allEntries.isEmpty()) "No sales data available" else "No entries match your filters",
                                fontSize = if (isCompact) 14.sp else 16.sp,
                                color = JivaColors.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    SalesTableRow(entry = entry)
                }

                // Total row
                item {
                    SalesTotalRow(
                        totalAmount = totalAmount,
                        totalQty = totalQty,
                        totalDiscount = totalDiscount,
                        totalSum = totalSum,
                        totalEntries = entries.size
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveActionButton(onClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JivaColors.Green
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) 16.dp else 24.dp,
            vertical = if (isCompact) 12.dp else 16.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share PDF",
            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SHARE PDF",
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isCompact) 14.sp else 16.sp
        )
    }
}

@Composable
private fun SalesTableHeader() {
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
        SalesHeaderCell("Date", Modifier.width(100.dp))
        SalesHeaderCell("Party", Modifier.width(150.dp))
        SalesHeaderCell("Type", Modifier.width(100.dp))
        SalesHeaderCell("Ref No", Modifier.width(80.dp))
        SalesHeaderCell("Item", Modifier.width(180.dp))
        SalesHeaderCell("HSN", Modifier.width(80.dp))
        SalesHeaderCell("Category", Modifier.width(120.dp))
        SalesHeaderCell("Qty", Modifier.width(80.dp))
        SalesHeaderCell("Unit", Modifier.width(80.dp))
        SalesHeaderCell("Rate", Modifier.width(100.dp))
        SalesHeaderCell("Amount", Modifier.width(120.dp))
        SalesHeaderCell("Discount", Modifier.width(100.dp))
        SalesHeaderCell("Total", Modifier.width(120.dp))
        SalesHeaderCell("CGST", Modifier.width(80.dp))
        SalesHeaderCell("SGST", Modifier.width(80.dp))
        SalesHeaderCell("IGST", Modifier.width(80.dp))
    }
}

@Composable
private fun SalesHeaderCell(text: String, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Text(
        text = text,
        fontSize = if (isCompact) 10.sp else 12.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SalesTableRow(entry: SalesReportEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                trDate = entry.trDate.takeIf { it.isNotBlank() } ?: "",
                partyName = entry.partyName.takeIf { it.isNotBlank() } ?: "Unknown",
                gstin = entry.gstin.takeIf { it.isNotBlank() } ?: "",
                entryType = entry.entryType.takeIf { it.isNotBlank() } ?: "",
                refNo = entry.refNo.takeIf { it.isNotBlank() } ?: "",
                itemName = entry.itemName.takeIf { it.isNotBlank() } ?: "Unknown Item",
                hsnNo = entry.hsnNo.takeIf { it.isNotBlank() } ?: "",
                itemType = entry.itemType.takeIf { it.isNotBlank() } ?: "",
                qty = entry.qty.takeIf { it.isNotBlank() } ?: "0",
                unit = entry.unit.takeIf { it.isNotBlank() } ?: "",
                rate = entry.rate.takeIf { it.isNotBlank() } ?: "0.00",
                amount = entry.amount.takeIf { it.isNotBlank() } ?: "0.00",
                discount = entry.discount.takeIf { it.isNotBlank() } ?: "0.00",
                total = entry.total.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.refNo}")
            SalesReportEntry("", "Error loading data", "", "", "", "", "", "", "0", "", "0.00", "0.00", "0.00", "", "", "", "0.00")
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
            SalesCell(safeEntry.trDate, Modifier.width(100.dp))
            SalesCell(safeEntry.partyName, Modifier.width(150.dp), JivaColors.DeepBlue)
            SalesCell(safeEntry.entryType, Modifier.width(100.dp), JivaColors.Purple)
            SalesCell(safeEntry.refNo, Modifier.width(80.dp))
            SalesCell(safeEntry.itemName, Modifier.width(180.dp), JivaColors.DarkGray)
            SalesCell(safeEntry.hsnNo, Modifier.width(80.dp))
            SalesCell(safeEntry.itemType, Modifier.width(120.dp), JivaColors.Orange)
            SalesCell(safeEntry.qty, Modifier.width(80.dp), JivaColors.DeepBlue)
            SalesCell(safeEntry.unit, Modifier.width(80.dp))
            SalesCell("₹${safeEntry.rate}", Modifier.width(100.dp))
            SalesCell(
                text = "₹${safeEntry.amount}",
                modifier = Modifier.width(120.dp),
                color = if (amountValue >= 0) JivaColors.Green else JivaColors.Red
            )
            SalesCell("₹${safeEntry.discount}", Modifier.width(100.dp), JivaColors.Orange)
            SalesCell("₹${safeEntry.total}", Modifier.width(120.dp), JivaColors.Green)
            SalesCell(safeEntry.cgst, Modifier.width(80.dp))
            SalesCell(safeEntry.sgst, Modifier.width(80.dp))
            SalesCell(safeEntry.igst, Modifier.width(80.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SalesCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
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
        fontSize = if (isCompact) 9.sp else 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SalesTotalRow(totalAmount: Double, totalQty: Double, totalDiscount: Double, totalSum: Double, totalEntries: Int) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Column {
        HorizontalDivider(
            color = JivaColors.DeepBlue,
            thickness = 2.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier
                .background(JivaColors.LightBlue.copy(alpha = 0.3f))
                .padding(vertical = if (isCompact) 8.dp else 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "TOTAL" text aligned with Date column
            Text(
                text = "TOTAL",
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )
            // Empty spaces for Party, Type, Ref No, Item, HSN, Category columns
            Box(modifier = Modifier.width(150.dp))
            Box(modifier = Modifier.width(100.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(180.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(120.dp))

            // Total quantity cell aligned with Qty column
            Text(
                text = "${String.format("%.2f", totalQty)}",
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )

            // Empty space for Unit column
            Box(modifier = Modifier.width(80.dp))

            // Empty space for Rate column
            Box(modifier = Modifier.width(100.dp))

            // Total amount cell aligned with Amount column
            Text(
                text = "₹${String.format("%.2f", totalAmount)}",
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Total discount cell aligned with Discount column
            Text(
                text = "₹${String.format("%.2f", totalDiscount)}",
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Orange,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total sum cell aligned with Total column
            Text(
                text = "₹${String.format("%.2f", totalSum)}",
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Purple,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )

            // Empty spaces for CGST, SGST, IGST columns
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(80.dp))
            Box(modifier = Modifier.width(80.dp))
        }
    }
}

/**
 * Generate and Share PDF for Sales Report with proper pagination and table formatting
 */
private suspend fun generateAndShareSalesPDF(
    context: android.content.Context,
    entries: List<SalesReportEntry>,
    totalAmount: Double,
    totalQty: Double,
    totalDiscount: Double,
    totalSum: Double
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
                textSize = 7f   // further reduced to fit all columns properly
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 5.5f   // further reduced to ensure no clipping
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
            val rowHeight = 16f   // reduced row height for better fit
            val headerHeight = 20f   // reduced header height

            // Calculate optimal column widths based on content
            val headers = listOf("Date", "Party", "Type", "Ref No", "Item", "HSN", "Category", "Qty", "Unit", "Rate", "Amount", "Discount", "Total", "CGST", "SGST", "IGST")
            val colWidths = calculateOptimalSalesColumnWidths(entries, headers, contentWidth, cellPaint)
            // Optimize column distribution for better fit
            // Reduce Party and Item columns to give more space to GST columns
            colWidths[1] = (colWidths[1] * 0.85f).coerceAtLeast(70f) // Party column
            colWidths[4] = (colWidths[4] * 0.75f).coerceAtLeast(65f) // Item column
            // Distribute freed width to GST columns and Total
            val freed = (colWidths[1] * 0.15f) + (colWidths[4] * 0.25f)
            val addToGst = (freed * 0.6f) / 3f  // 60% to GST columns
            val addToTotal = freed * 0.4f       // 40% to Total column
            colWidths[12] += addToTotal // Total column
            colWidths[13] += addToGst   // CGST column
            colWidths[14] += addToGst   // SGST column  
            colWidths[15] += addToGst   // IGST column

            // Calculate how many rows fit per page
            val titleBlockHeight = 30f + 20f + 15f + 25f // title + generated + page + spacing
            val availableHeightBase = pageHeight - titleBlockHeight - headerHeight - margin - margin
            val rowsPerPage = (availableHeightBase / rowHeight).toInt().coerceAtLeast(1)

            var currentPage = 1
            var entryIndex = 0
            val totalPages = ((entries.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)

            while (entryIndex < entries.size) {
                val page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create())
                val canvas = page.canvas

                var currentY = 30f

                // Title and page info
                canvas.drawText("Sales Report", (pageWidth / 2).toFloat(), currentY, titlePaint)
                currentY += 20f
                canvas.drawText("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 15f
                canvas.drawText("Page $currentPage of $totalPages", (pageWidth / 2).toFloat(), currentY, cellPaint)
                currentY += 25f

                // Summary section (only on first page)
                if (currentPage == 1) {
                    canvas.drawText("Summary:", startX, currentY, headerPaint)
                    currentY += 15f
                    canvas.drawText("Total Transactions: ${entries.size}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Total Amount: ₹${String.format("%.2f", totalAmount)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Total Quantity: ${String.format("%.2f", totalQty)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Total Discount: ₹${String.format("%.2f", totalDiscount)}", startX, currentY, cellPaint)
                    currentY += 12f
                    canvas.drawText("Grand Total: ₹${String.format("%.2f", totalSum)}", startX, currentY, cellPaint)
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
                    drawSalesTextCentered(canvas, headers[i], xCursor + colWidths[i]/2, headerBottom - 6f, colWidths[i] - 10f, headerPaint)
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
                        entry.trDate,
                        entry.partyName,
                        entry.entryType,
                        entry.refNo,
                        entry.itemName,
                        entry.hsnNo,
                        entry.itemType,
                        entry.qty,
                        entry.unit,
                        "₹${entry.rate}",
                        "₹${entry.amount}",
                        "₹${entry.discount}",
                        "₹${entry.total}",
                        entry.cgst,
                        entry.sgst,
                        entry.igst
                    )
                    
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight
                    for (j in data.indices) {
                        val rect = android.graphics.RectF(xCursor, rowTop, xCursor + colWidths[j], rowBottom)
                        canvas.drawRect(rect, borderPaint)
                        drawSalesTextCentered(canvas, data[j], xCursor + colWidths[j]/2, rowBottom - 4f, colWidths[j] - 10f, cellPaint)
                        xCursor += colWidths[j]
                    }
                    currentY = rowBottom
                }

                // Total row on last page
                if (currentPage == totalPages && currentY + rowHeight <= bottomY) {
                    val totalTop = currentY + 8f
                    val totalBottom = totalTop + rowHeight
                    val totalRect = android.graphics.RectF(startX, totalTop, startX + contentWidth, totalBottom)
                    canvas.drawRect(totalRect, fillHeaderPaint)
                    canvas.drawRect(totalRect, borderPaint)
                    drawSalesTextCentered(canvas, "TOTAL: ${entries.size} transactions | Amount: ₹${String.format("%.2f", totalAmount)} | Qty: ${String.format("%.2f", totalQty)} | Discount: ₹${String.format("%.2f", totalDiscount)} | Grand Total: ₹${String.format("%.2f", totalSum)}", startX + contentWidth/2, totalBottom - 4f, contentWidth - 10f, headerPaint)
                }

                pdfDocument.finishPage(page)
                entryIndex = endIndex
                currentPage++
            }

            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Sales_Report_$timestamp.pdf"
            val file = java.io.File(downloadsDir, fileName)

            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "PDF saved to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
                shareSalesPDF(context, file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * Calculate optimal column widths based on content for Sales Report
 */
private fun calculateOptimalSalesColumnWidths(
    entries: List<SalesReportEntry>,
    headers: List<String>,
    totalWidth: Float,
    paint: android.graphics.Paint
): FloatArray {
    val colWidths = FloatArray(headers.size)

    // Base weights for columns to stabilize layout on A4 landscape - optimized for GST columns
    val weights = floatArrayOf(
        0.065f, // Date
        0.12f,  // Party (reduced)
        0.065f, // Type
        0.045f, // Ref No
        0.13f,  // Item (reduced)
        0.045f, // HSN
        0.065f, // Category
        0.045f, // Qty
        0.035f, // Unit
        0.055f, // Rate
        0.075f, // Amount
        0.065f, // Discount
        0.085f, // Total (increased)
        0.05f,  // CGST (increased)
        0.05f,  // SGST (increased)
        0.05f   // IGST (increased)
    )

    // Normalize weights in case of drift
    val weightSum = weights.sum()
    val normalized = weights.map { it / weightSum }

    // Compute target widths by weights
    for (i in headers.indices) {
        colWidths[i] = (totalWidth * normalized[i]).coerceAtLeast(40f)
    }

    return colWidths
}

/**
 * Draw text centered in a cell for Sales Report
 */
private fun drawSalesTextCentered(
    canvas: android.graphics.Canvas,
    text: String,
    centerX: Float,
    y: Float,
    maxWidth: Float,
    paint: android.graphics.Paint
) {
    // Ellipsize by measuring progressively
    var display = text
    while (paint.measureText(display) > maxWidth && display.isNotEmpty()) {
        // Remove a chunk to speed up shrinking
        val remove = ((paint.measureText(display) - maxWidth) / (paint.textSize / 1.8f)).toInt().coerceAtLeast(1)
        val newLen = (display.length - remove).coerceAtLeast(0)
        display = if (newLen > 3) display.substring(0, newLen - 3) + "..." else "..."
    }
    val x = centerX - paint.measureText(display) / 2
    canvas.drawText(display, x, y, paint)
}

/**
 * Share PDF file for Sales Report
 */
private fun shareSalesPDF(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sales Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the Sales Report attached.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Grant URI permission to potential receivers
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Sales Report").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}