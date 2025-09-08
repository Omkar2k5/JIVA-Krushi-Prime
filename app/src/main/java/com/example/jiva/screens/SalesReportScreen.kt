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
                    discount = entity.discount?.toString() ?: "0.00"
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping sale/purchase entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(partyNameSearch, itemNameSearch, allSalesEntries) {
        try {
            if (allSalesEntries.isEmpty()) {
                emptyList()
            } else {
                allSalesEntries.filter { entry ->
                    try {
                        // Party Name Search Filter
                        val partyMatch = if (partyNameSearch.isBlank()) true else
                            entry.partyName.contains(partyNameSearch, ignoreCase = true)

                        // Item Name Search Filter
                        val itemMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)

                        partyMatch && itemMatch
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
        // Responsive Header
        ResponsiveReportHeader(
            title = "Sales Report",
            subtitle = "Transaction history and sales data",
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

                        // Party Name Search - First line
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
                                text = "Sales Transactions",
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

                        // Outstanding Report style table with horizontal scrolling
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header in Outstanding Report style
                            SalesTableHeader()

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
                                                    text = if (allSalesEntries.isEmpty()) "No sales data available" else "No entries match your filters",
                                                    fontSize = 16.sp,
                                                    color = JivaColors.DarkGray,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (allSalesEntries.isEmpty()) {
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
                                        SalesTableRow(entry = entry)
                                    }

                                    // Total row like Outstanding Report
                                    item {
                                        SalesTotalRow(
                                            totalAmount = totalAmount,
                                            totalQty = totalQty,
                                            totalDiscount = totalDiscount,
                                            totalEntries = filteredEntries.size
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val pdfData = generateSalesReportPDF(filteredEntries, totalAmount, totalQty, totalDiscount)
                                        sharePDF(context, pdfData, "Sales_Report_${System.currentTimeMillis()}.pdf")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JivaColors.Green
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
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
                    }
                }
            }
        }
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
        SalesHeaderCell("GSTIN", Modifier.width(150.dp))
    }
}

@Composable
private fun SalesHeaderCell(text: String, modifier: Modifier = Modifier) {
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
                discount = entry.discount.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.refNo}")
            SalesReportEntry("", "Error loading data", "", "", "", "", "", "", "0", "", "0.00", "0.00", "0.00")
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
            SalesCell(safeEntry.gstin, Modifier.width(150.dp))
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
private fun SalesTotalRow(totalAmount: Double, totalQty: Double, totalDiscount: Double, totalEntries: Int) {
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

/**
 * Generate PDF for Sales Report
 */
private suspend fun generateSalesReportPDF(
    entries: List<SalesReportEntry>,
    totalAmount: Double,
    totalQty: Double,
    totalDiscount: Double
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
            canvas.drawText("Sales Report", 40f, y, paint)
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
            canvas.drawText("Total Entries: ${entries.size}", 40f, y, paint)
            y += 14f
            canvas.drawText("Total Amount: ₹${String.format("%.2f", totalAmount)}", 40f, y, paint)
            y += 14f
            canvas.drawText("Total Quantity: ${String.format("%.2f", totalQty)}", 40f, y, paint)
            y += 14f
            canvas.drawText("Total Discount: ₹${String.format("%.2f", totalDiscount)}", 40f, y, paint)

            y += 20f
            paint.isFakeBoldText = true
            canvas.drawText("Detailed Data:", 40f, y, paint)
            paint.isFakeBoldText = false
            y += 16f

            val headers = listOf("Date", "Party", "Type", "Ref", "Item", "HSN", "Category", "Qty", "Unit", "Rate", "Amount", "Discount")
            val colX = floatArrayOf(40f, 90f, 150f, 190f, 230f, 300f, 350f, 410f, 440f, 470f, 510f, 550f)
            headers.forEachIndexed { idx, h -> canvas.drawText(h, colX[idx], y, paint) }
            y += 12f
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 12f

            entries.take(35).forEach { entry ->
                if (y > 800f) return@forEach
                canvas.drawText(entry.trDate, colX[0], y, paint)
                canvas.drawText(entry.partyName.take(16), colX[1], y, paint)
                canvas.drawText(entry.entryType, colX[2], y, paint)
                canvas.drawText(entry.refNo, colX[3], y, paint)
                canvas.drawText(entry.itemName.take(16), colX[4], y, paint)
                canvas.drawText(entry.hsnNo, colX[5], y, paint)
                canvas.drawText(entry.itemType.take(12), colX[6], y, paint)
                canvas.drawText(entry.qty, colX[7], y, paint)
                canvas.drawText(entry.unit, colX[8], y, paint)
                canvas.drawText(entry.rate, colX[9], y, paint)
                canvas.drawText(entry.amount, colX[10], y, paint)
                canvas.drawText(entry.discount, colX[11], y, paint)
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
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sales Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Please find the attached Sales Report.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Sales Report"))
    } catch (e: Exception) {
        timber.log.Timber.e(e, "Error sharing PDF")
        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}