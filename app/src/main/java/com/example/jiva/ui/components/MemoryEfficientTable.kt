package com.example.jiva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import com.example.jiva.screens.StockEntry
import com.example.jiva.utils.LowEndDeviceOptimizer
import kotlinx.coroutines.delay

/**
 * Memory-Efficient Table for Low-End Devices
 * Optimized for large datasets with minimal memory usage
 */
@Composable
fun MemoryEfficientStockTable(
    paginatedData: LowEndDeviceOptimizer.PaginatedDataState<StockEntry>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-load more data when near end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= paginatedData.visibleItems.size - 5 && 
                    paginatedData.hasMorePages && 
                    !paginatedData.isLoading) {
                    onLoadMore()
                }
            }
    }
    
    Column(modifier = modifier) {
        // Progress indicator during filtering
        if (paginatedData.isLoading && paginatedData.filterProgress > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = paginatedData.filterProgress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = JivaColors.DeepBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = paginatedData.filterMessage,
                        fontSize = 12.sp,
                        color = JivaColors.DarkGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Data summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing: ${paginatedData.visibleItems.size} of ${paginatedData.totalItems}",
                    color = JivaColors.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                if (paginatedData.hasMorePages) {
                    Text(
                        text = "Scroll for more",
                        color = JivaColors.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Memory-efficient table
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = JivaColors.White)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                // Table header
                item {
                    MemoryEfficientTableHeader()
                }
                
                // Table rows with memory optimization
                itemsIndexed(
                    items = paginatedData.visibleItems,
                    key = { index, item -> "${item.itemId}_$index" }
                ) { index, entry ->
                    MemoryEfficientTableRow(
                        entry = entry,
                        index = index
                    )
                }
                
                // Loading indicator at bottom
                if (paginatedData.hasMorePages) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (paginatedData.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = JivaColors.DeepBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Scroll to load more...",
                                    fontSize = 12.sp,
                                    color = JivaColors.DarkGray
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
private fun MemoryEfficientTableHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("ID", 60.dp)
            HeaderCell("Item Name", 120.dp)
            HeaderCell("Opening", 70.dp)
            HeaderCell("Closing", 70.dp)
            HeaderCell("Valuation", 80.dp)
            HeaderCell("Company", 100.dp)
        }
    }
}

@Composable
private fun MemoryEfficientTableRow(
    entry: StockEntry,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) JivaColors.White else JivaColors.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataCell(entry.itemId, 60.dp)
            DataCell(entry.itemName, 120.dp)
            DataCell(entry.opening, 70.dp)
            DataCell(entry.closingStock, 70.dp)
            DataCell("₹${entry.valuation}", 80.dp)
            DataCell(entry.company, 100.dp)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = JivaColors.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DataCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = JivaColors.DarkGray,
        fontSize = 9.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Emergency fallback table for extremely low memory situations
 */
@Composable
fun EmergencyStockTable(
    entries: List<StockEntry>,
    maxItems: Int = 20
) {
    val limitedEntries = entries.take(maxItems)
    
    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = JivaColors.Orange.copy(alpha = 0.1f))
        ) {
            Text(
                text = "⚠️ Emergency mode: Showing ${limitedEntries.size} of ${entries.size} items",
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                color = JivaColors.Orange,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(limitedEntries.size) { index ->
                val entry = limitedEntries[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "${entry.itemId} - ${entry.itemName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = JivaColors.DeepBlue
                        )
                        Text(
                            text = "Stock: ${entry.closingStock} | Value: ₹${entry.valuation}",
                            fontSize = 10.sp,
                            color = JivaColors.DarkGray
                        )
                    }
                }
            }
        }
    }
}
