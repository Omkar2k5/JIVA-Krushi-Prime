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
    val discount: String
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
    var transactionTypeFilter by remember { mutableStateOf("All Types") }
    var partyNameSearch by remember { mutableStateOf("") }
    var itemNameSearch by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All Categories") }
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
                    discount = entity.discount?.toString() ?: "0.00"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping sale/purchase entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(transactionTypeFilter, partyNameSearch, itemNameSearch, categoryFilter, allSalesEntries) {
        try {
            if (allSalesEntries.isEmpty()) {
                emptyList()
            } else {
                allSalesEntries.filter { entry ->
                    try {
                        // Transaction Type Filter
                        val typeMatch = when (transactionTypeFilter) {
                            "All Types" -> true
                            else -> entry.entryType.equals(transactionTypeFilter, ignoreCase = true)
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
                            else -> entry.itemType.equals(categoryFilter, ignoreCase = true)
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
            title = "Sales Report",
            subtitle = "Transaction history and sales data",
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
                                        Toast.makeText(context, "✅ Sales data refreshed successfully", Toast.LENGTH_SHORT).show()
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

        // Simple content for now
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sales Summary",
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

                        if (filteredEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
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
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = if (allSalesEntries.isEmpty()) "No sales data available" else "No entries match your filters",
                                        fontSize = 14.sp,
                                        color = JivaColors.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                    if (allSalesEntries.isEmpty()) {
                                        Text(
                                            text = "Tap the refresh button to sync data",
                                            fontSize = 12.sp,
                                            color = JivaColors.Purple,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                }
            }
        }
    }
}