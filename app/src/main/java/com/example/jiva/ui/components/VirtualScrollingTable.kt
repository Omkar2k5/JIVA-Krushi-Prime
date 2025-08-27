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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.ui.theme.JivaColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * High-Performance Virtual Scrolling Table for Large Datasets
 * Only renders visible items for optimal performance
 */
@Composable
fun <T> VirtualScrollingTable(
    data: List<T>,
    columns: List<TableColumn<T>>,
    modifier: Modifier = Modifier,
    itemHeight: Int = 50,
    headerHeight: Int = 60,
    visibleItemsBuffer: Int = 10,
    onItemClick: (T) -> Unit = {},
    loadingProgress: Float = 0f,
    loadingMessage: String = "",
    isLoading: Boolean = false
) {
    val listState = rememberLazyListState()
    
    // Calculate visible range for optimization
    val visibleRange by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            val start = maxOf(0, firstVisible - visibleItemsBuffer)
            val end = minOf(data.size - 1, lastVisible + visibleItemsBuffer)
            
            start..end
        }
    }
    
    Column(modifier = modifier) {
        // Loading Progress Bar
        if (isLoading && loadingProgress > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JivaColors.White)
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = loadingProgress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = JivaColors.DeepBlue,
                    trackColor = JivaColors.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loadingMessage,
                    fontSize = 12.sp,
                    color = JivaColors.DarkGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Table Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JivaColors.DeepBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                columns.forEach { column ->
                    Text(
                        text = column.title,
                        color = JivaColors.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(column.width)
                    )
                }
            }
        }
        
        // Virtual Scrolling Table Body
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(
                items = data,
                key = { index, item -> "${column.keyExtractor(item)}_$index" }
            ) { index, item ->
                // Only render if in visible range (virtual scrolling)
                if (index in visibleRange || data.size < 100) {
                    VirtualTableRow(
                        item = item,
                        columns = columns,
                        index = index,
                        itemHeight = itemHeight,
                        onClick = { onItemClick(item) }
                    )
                } else {
                    // Placeholder for non-visible items
                    Spacer(modifier = Modifier.height(itemHeight.dp))
                }
            }
        }
        
        // Data Summary
        if (!isLoading && data.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray)
            ) {
                Text(
                    text = "Showing ${data.size} records • Virtual scrolling active",
                    fontSize = 10.sp,
                    color = JivaColors.DarkGray,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun <T> VirtualTableRow(
    item: T,
    columns: List<TableColumn<T>>,
    index: Int,
    itemHeight: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) JivaColors.White else JivaColors.LightGray.copy(alpha = 0.3f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { column ->
                Text(
                    text = column.valueExtractor(item),
                    fontSize = 10.sp,
                    color = JivaColors.DarkGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(column.width)
                )
            }
        }
    }
}

/**
 * Table Column Configuration
 */
data class TableColumn<T>(
    val title: String,
    val width: androidx.compose.ui.unit.Dp,
    val valueExtractor: (T) -> String,
    val keyExtractor: (T) -> String = { valueExtractor(it) }
)

/**
 * Progressive Loading Table with Chunked Data Display
 */
@Composable
fun <T> ProgressiveLoadingTable(
    dataFlow: kotlinx.coroutines.flow.Flow<List<T>>,
    columns: List<TableColumn<T>>,
    modifier: Modifier = Modifier,
    onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
) {
    var currentData by remember { mutableStateOf<List<T>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    
    // Collect data progressively
    LaunchedEffect(dataFlow) {
        dataFlow.collect { newData ->
            currentData = newData
            if (newData.isNotEmpty()) {
                isLoading = false
            }
        }
    }
    
    VirtualScrollingTable(
        data = currentData,
        columns = columns,
        modifier = modifier,
        isLoading = isLoading,
        loadingProgress = progress,
        loadingMessage = progressMessage
    )
}

/**
 * Optimized Outstanding Table
 */
@Composable
fun OptimizedOutstandingTable(
    dataFlow: kotlinx.coroutines.flow.Flow<List<com.example.jiva.data.database.entities.OutstandingEntity>>,
    modifier: Modifier = Modifier,
    onItemClick: (com.example.jiva.data.database.entities.OutstandingEntity) -> Unit = {}
) {
    val columns = remember {
        listOf(
            TableColumn<com.example.jiva.data.database.entities.OutstandingEntity>(
                title = "AC ID",
                width = 80.dp,
                valueExtractor = { it.acId },
                keyExtractor = { it.acId }
            ),
            TableColumn(
                title = "Account Name",
                width = 180.dp,
                valueExtractor = { it.accountName }
            ),
            TableColumn(
                title = "Mobile",
                width = 140.dp,
                valueExtractor = { it.mobile }
            ),
            TableColumn(
                title = "Balance",
                width = 120.dp,
                valueExtractor = { "₹${it.balance}" }
            ),
            TableColumn(
                title = "Days",
                width = 80.dp,
                valueExtractor = { it.days }
            ),
            TableColumn(
                title = "Last Date",
                width = 140.dp,
                valueExtractor = { it.lastDate }
            )
        )
    }
    
    ProgressiveLoadingTable(
        dataFlow = dataFlow,
        columns = columns,
        modifier = modifier
    )
}

/**
 * Optimized Stock Table
 */
@Composable
fun OptimizedStockTable(
    dataFlow: kotlinx.coroutines.flow.Flow<List<com.example.jiva.data.database.entities.StockEntity>>,
    modifier: Modifier = Modifier,
    onItemClick: (com.example.jiva.data.database.entities.StockEntity) -> Unit = {}
) {
    val columns = remember {
        listOf(
            TableColumn<com.example.jiva.data.database.entities.StockEntity>(
                title = "Item ID",
                width = 80.dp,
                valueExtractor = { it.itemId },
                keyExtractor = { it.itemId }
            ),
            TableColumn(
                title = "Item Name",
                width = 150.dp,
                valueExtractor = { it.itemName }
            ),
            TableColumn(
                title = "Opening",
                width = 80.dp,
                valueExtractor = { it.opening }
            ),
            TableColumn(
                title = "Closing",
                width = 80.dp,
                valueExtractor = { it.closingStock }
            ),
            TableColumn(
                title = "Valuation",
                width = 100.dp,
                valueExtractor = { "₹${it.valuation}" }
            ),
            TableColumn(
                title = "Company",
                width = 100.dp,
                valueExtractor = { it.company }
            )
        )
    }
    
    ProgressiveLoadingTable(
        dataFlow = dataFlow,
        columns = columns,
        modifier = modifier
    )
}
