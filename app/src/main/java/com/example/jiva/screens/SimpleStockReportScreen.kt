package com.example.jiva.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.StockReportViewModel
import kotlinx.coroutines.launch

/**
 * Simple Stock Report Screen - Fallback version for debugging crashes
 * This version uses minimal components and basic functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleStockReportScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: StockReportViewModel = viewModel(
        factory = StockReportViewModel.Factory(application.database)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Simple state management
    var isLoading by remember { mutableStateOf(true) }
    var stockData by remember { mutableStateOf<List<StockEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get current year
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    
    // Load data with error handling
    LaunchedEffect(year) {
        try {
            isLoading = true
            errorMessage = null
            
            // Simple data loading
            val stockEntities = viewModel.observeStock(year).value ?: emptyList()
            
            stockData = stockEntities.mapNotNull { entity ->
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
                    timber.log.Timber.e(e, "Error mapping entity")
                    null
                }
            }
            
            isLoading = false
            
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error loading stock data")
            errorMessage = "Error loading data: ${e.message}"
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        ResponsiveReportHeader(
            title = "Stock Report (Simple)",
            onBackClick = { /* Handle back */ },
            onRefreshClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    // Trigger reload
                }
            },
            isRefreshing = isLoading
        )
        
        // Content
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = JivaColors.DeepBlue
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Stock Data...",
                            fontSize = 16.sp,
                            color = JivaColors.DarkGray
                        )
                    }
                }
            }
            
            errorMessage != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = JivaColors.Orange
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error Loading Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )
                        Text(
                            text = errorMessage!!,
                            fontSize = 14.sp,
                            color = JivaColors.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                isLoading = true
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JivaColors.DeepBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
            
            stockData.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "No Data",
                            modifier = Modifier.size(64.dp),
                            tint = JivaColors.DarkGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Stock Data",
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
                    }
                }
            }
            
            else -> {
                // Data loaded successfully - show simple list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Stock Summary",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JivaColors.White
                                )
                                Text(
                                    text = "Total Items: ${stockData.size}",
                                    fontSize = 14.sp,
                                    color = JivaColors.White
                                )
                                Text(
                                    text = "Year: $year",
                                    fontSize = 12.sp,
                                    color = JivaColors.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    // Stock items (limited to first 50 for performance)
                    items(stockData.take(50)) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "${item.itemId} - ${item.itemName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Stock: ${item.closingStock}",
                                        fontSize = 12.sp,
                                        color = JivaColors.DarkGray
                                    )
                                    Text(
                                        text = "Value: ₹${item.valuation}",
                                        fontSize = 12.sp,
                                        color = JivaColors.Green
                                    )
                                }
                                if (item.companyName.isNotBlank()) {
                                    Text(
                                        text = "Company: ${item.companyName}",
                                        fontSize = 10.sp,
                                        color = JivaColors.DarkGray.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show message if data is limited
                    if (stockData.size > 50) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = JivaColors.Orange.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = "Showing first 50 of ${stockData.size} items for performance",
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 12.sp,
                                    color = JivaColors.Orange,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
