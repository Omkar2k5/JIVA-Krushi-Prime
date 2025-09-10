package com.example.jiva.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import timber.log.Timber

/**
 * Unified Table Components to eliminate duplicate HeaderCell/DataCell implementations
 * across multiple screens. This replaces individual implementations in:
 * - SalesReportScreen (SalesHeaderCell, SalesCell)
 * - StockReportScreen (StockHeaderCell, StockCell) 
 * - PriceListReportScreen (PriceListHeaderCell, PriceListCell)
 * - WhatsAppBulkMessageScreen (CustomerHeaderCell, CustomerCell)
 * - OutstandingReportScreen (HeaderCell, DataCell)
 */

/**
 * Unified responsive header cell component
 */
@Composable
fun UnifiedHeaderCell(
    text: String, 
    modifier: Modifier = Modifier,
    backgroundColor: Color = JivaColors.LightBlue.copy(alpha = 0.3f),
    textColor: Color = JivaColors.DeepBlue
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Text(
        text = text,
        fontSize = if (isCompact) 10.sp else 12.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(backgroundColor)
            .padding(8.dp)
    )
}

/**
 * Unified responsive data cell component
 */
@Composable
fun UnifiedDataCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151),
    backgroundColor: Color = Color.Transparent
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    // Safe text processing
    val safeText = remember(text) {
        try {
            text.takeIf { it.isNotBlank() } ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Error processing text: $text")
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
            .background(backgroundColor)
            .padding(8.dp)
    )
}

/**
 * Unified table header row component
 */
@Composable
fun UnifiedTableHeader(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(JivaColors.LightBlue.copy(alpha = 0.3f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEach { column ->
            UnifiedHeaderCell(
                text = column.title,
                modifier = Modifier.width(column.width)
            )
        }
    }
}

/**
 * Unified table row component
 */
@Composable
fun UnifiedTableRow(
    data: List<String>,
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent
) {
    Row(
        modifier = modifier
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        data.forEachIndexed { index, cellData ->
            if (index < columns.size) {
                UnifiedDataCell(
                    text = cellData,
                    modifier = Modifier.width(columns[index].width)
                )
            }
        }
    }
}

/**
 * Complete unified table component with scrolling
 */
@Composable
fun <T> UnifiedTable(
    data: List<T>,
    columns: List<TableColumn>,
    dataMapper: (T) -> List<String>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 400.dp
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    val tableHeight = if (isCompact) 350.dp else maxHeight

    Column(
        modifier = modifier
    ) {
        // Header
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            UnifiedTableHeader(columns = columns)
        }
        
        HorizontalDivider(color = JivaColors.DeepBlue, thickness = 1.dp)
        
        // Data rows with scrolling
        LazyColumn(
            modifier = Modifier
                .height(tableHeight)
                .horizontalScroll(rememberScrollState())
        ) {
            items(data) { item ->
                val rowData = dataMapper(item)
                UnifiedTableRow(
                    data = rowData,
                    columns = columns,
                    backgroundColor = Color.Transparent
                )
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

/**
 * Table column definition
 */
data class TableColumn(
    val title: String,
    val width: Dp,
    val alignment: TextAlign = TextAlign.Center
)

/**
 * Unified total row component
 */
@Composable
fun UnifiedTotalRow(
    columns: List<TableColumn>,
    totalData: List<String>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp.dp < 600.dp
    
    Column {
        HorizontalDivider(
            color = JivaColors.DeepBlue,
            thickness = 2.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = modifier
                .background(JivaColors.LightBlue.copy(alpha = 0.3f))
                .padding(vertical = if (isCompact) 8.dp else 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            totalData.forEachIndexed { index, data ->
                if (index < columns.size) {
                    Text(
                        text = data,
                        fontSize = if (isCompact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            data.contains("Total:") && data.contains("₹") -> JivaColors.Green
                            data.contains("Total:") -> JivaColors.DeepBlue
                            else -> JivaColors.Orange
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(columns[index].width)
                    )
                }
            }
        }
    }
}
