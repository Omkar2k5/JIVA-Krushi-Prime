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
                ExpiryEntry(
                    itemId = entity.itemId?.toString() ?: "",
                    itemName = entity.itemName ?: "",
                    itemType = entity.itemType ?: "",
                    batchNo = entity.batchNo ?: "",
                    expiryDate = entity.expiryDate?.let { 
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) 
                    } ?: "",
                    qty = entity.qty?.toString() ?: "0",
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
                        Text(
                            text = "Expiry Summary",
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
                        }
                    }
                }
            }
        }
    }
}
