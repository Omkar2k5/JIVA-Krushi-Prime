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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.StockReportViewModel
import kotlinx.coroutines.launch
import com.example.jiva.utils.PDFGenerator

// Data model for Stock Report entries - All String for faster retrieval
data class StockEntry(
    val itemId: String,
    val itemName: String,
    val openingStock: String,
    val inQty: String,
    val outQty: String,
    val closingStock: String,
    val avgRate: String,
    val valuation: String,
    val itemType: String,
    val companyName: String,
    val cgst: String,
    val sgst: String,
    val igst: String // Added IGST field
)

@Composable
fun StockReportScreen(onBackClick: () -> Unit = {}) {
    StockReportScreenImpl(onBackClick = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication

    // Create the ViewModel with the repository
    val viewModel: StockReportViewModel = viewModel(
        factory = StockReportViewModel.Factory(application.database)
    )

    val uiState by viewModel.uiState.collectAsState()

    // State management
    var stockOf by remember { mutableStateOf("All Items") }
    var viewAll by remember { mutableStateOf(false) }
    var itemNameSearch by remember { mutableStateOf("") }
    var companySearch by remember { mutableStateOf("") }
    var isStockDropdownExpanded by remember { mutableStateOf(false) }
    var isItemTypeDropdownExpanded by remember { mutableStateOf(false) }
    // Additional local states used in input fields
    var brandName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var selectedItemType by remember { mutableStateOf("All") }
    var packagingSize by remember { mutableStateOf("") }
    var exempted by remember { mutableStateOf(false) }

    // WhatsApp messaging state
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // WhatsApp template message
    val whatsappTemplate = "Hello Kurshi Prime"

    // Sync + observe Stock table
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Note: Removed test environment initialization to prevent conflicts
    // Data loading is now handled automatically by AppDataLoader at app startup
    // No manual loading needed here - data is already available

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Optimized data loading - only from Room DB for better performance
    val stockEntities by viewModel.observeStock(year).collectAsState(initial = emptyList())

    // Use only Stock DB data - Direct string mapping for fastest performance
    val allStockEntries = remember(stockEntities) {
        try {
            stockEntities.map { entity ->
                StockEntry(
                    itemId = entity.itemId,
                    itemName = entity.itemName,
                    openingStock = entity.opening,
                    inQty = entity.inWard,
                    outQty = entity.outWard,
                    closingStock = entity.closingStock,
                    avgRate = entity.avgRate,
                    valuation = entity.valuation,
                    itemType = entity.itemType,
                    companyName = entity.company,
                    cgst = entity.cgst,
                    sgst = entity.sgst,
                    igst = entity.igst
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping stock entities")
            emptyList()
        }
    }

    // Item type options
    val itemTypeOptions = listOf("All", "General", "Pesticides", "Fertilizers", "PGR", "Seeds")

    // Optimized filtering with error handling
    val filteredEntries = remember(itemNameSearch, companySearch, allStockEntries) {
        try {
            if (allStockEntries.isEmpty()) {
                emptyList()
            } else {
                allStockEntries.filter { entry ->
                    try {
                        val nameMatch = if (itemNameSearch.isBlank()) true else
                            entry.itemName.contains(itemNameSearch, ignoreCase = true)
                        val companyMatch = if (companySearch.isBlank()) true else
                            entry.companyName.contains(companySearch, ignoreCase = true)
                        nameMatch && companyMatch
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

    // Handle select all functionality
    LaunchedEffect(selectAll, filteredEntries) {
        if (selectAll) {
            selectedEntries = filteredEntries.map { it.itemId }.toSet()
        } else {
            selectedEntries = emptySet()
        }
    }

    // Update selectAll state based on individual selections
    LaunchedEffect(selectedEntries, filteredEntries) {
        selectAll = filteredEntries.isNotEmpty() && selectedEntries.containsAll(filteredEntries.map { it.itemId })
    }

    // Calculate totals from string values
    val totalOpeningStock = remember(filteredEntries) {
        filteredEntries.sumOf { it.openingStock.toDoubleOrNull() ?: 0.0 }
    }
    val totalInQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.inQty.toDoubleOrNull() ?: 0.0 }
    }
    val totalOutQty = remember(filteredEntries) {
        filteredEntries.sumOf { it.outQty.toDoubleOrNull() ?: 0.0 }
    }
    val totalClosingStock = remember(filteredEntries) {
        filteredEntries.sumOf { it.closingStock.toDoubleOrNull() ?: 0.0 }
    }
    val totalValuation = remember(filteredEntries) {
        filteredEntries.sumOf {
            val cleanValuation = it.valuation
                .replace("₹", "")
                .replace(",", "")
                .replace(" ", "")
                .trim()
            cleanValuation.toDoubleOrNull() ?: 0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header with Refresh Button
        ResponsiveReportHeader(
            title = "Stock Report",
            subtitle = "Inventory management and stock analysis",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = {
                        if (finalUserId != null) {
                            isLoading = true
                            scope.launch {
                                viewModel.refreshStockData(finalUserId, year, context)
                                kotlinx.coroutines.delay(1000) // Show loading for at least 1 second
                                isLoading = false
                            }
                        }
                    },
                    enabled = finalUserId != null && !isLoading,
                    modifier = Modifier
                        .background(
                            JivaColors.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = JivaColors.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Stock Data",
                            tint = JivaColors.White
                        )
                    }
                }
            }
        )

        // Main content with performance optimizations
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
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
                            text = "Stock Filter Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // First row: Item Code, Brand Name, Item Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemNameSearch,
                                    onValueChange = { itemNameSearch = it },
                                    placeholder = { Text("Search by item name...") },
                                    trailingIcon = {
                                        IconButton(onClick = { itemNameSearch = "" }) {
                                            Icon(
                                                imageVector = if (itemNameSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                                                contentDescription = if (itemNameSearch.isNotEmpty()) "Clear" else "Search"
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = JivaColors.DeepBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Brand Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = brandName,
                                    onValueChange = { brandName = it },
                                    placeholder = { Text("Enter brand name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = { itemName = it },
                                    placeholder = { Text("Enter item name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Second row: Item Description, Item Type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Item Description",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = itemDescription,
                                    onValueChange = { itemDescription = it },
                                    placeholder = { Text("Enter item description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

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
                                        value = selectedItemType,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isItemTypeDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isItemTypeDropdownExpanded,
                                        onDismissRequest = { isItemTypeDropdownExpanded = false }
                                    ) {
                                        itemTypeOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedItemType = option
                                                    isItemTypeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Third row: Company Name, Packaging Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Company Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = companySearch,
                                    onValueChange = { companySearch = it },
                                    placeholder = { Text("Search by company...") },
                                    trailingIcon = {
                                        IconButton(onClick = { companySearch = "" }) {
                                            Icon(
                                                imageVector = if (companySearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Business,
                                                contentDescription = if (companySearch.isNotEmpty()) "Clear" else "Company"
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = JivaColors.DeepBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Packaging Size",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = packagingSize,
                                    onValueChange = { packagingSize = it },
                                    placeholder = { Text("Enter packaging size") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Exempted checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = exempted,
                                onCheckedChange = { exempted = it },
                                colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                            )
                            Text(
                                text = "Exempted Items",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: Show filtered results */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Show",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHOW",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val columns = listOf(
                                            PDFGenerator.TableColumn("Item ID", 80f) { (it as StockEntry).itemId },
                                            PDFGenerator.TableColumn("Item Name", 150f) { (it as StockEntry).itemName },
                                            PDFGenerator.TableColumn("Opening", 80f) { "${(it as StockEntry).openingStock.toInt()}" },
                                            PDFGenerator.TableColumn("In Qty", 80f) { "${(it as StockEntry).inQty.toInt()}" },
                                            PDFGenerator.TableColumn("Out Qty", 80f) { "${(it as StockEntry).outQty.toInt()}" },
                                            PDFGenerator.TableColumn("Closing", 80f) { "${(it as StockEntry).closingStock.toInt()}" },
                                            PDFGenerator.TableColumn("Valuation", 100f) { "₹${String.format("%.2f", (it as StockEntry).valuation)}" }
                                        )

                                        val totalRow = mapOf(
                                            "Item ID" to "TOTAL",
                                            "Item Name" to "",
                                            "Opening" to "${totalOpeningStock.toInt()}",
                                            "In Qty" to "${totalInQty.toInt()}",
                                            "Out Qty" to "${totalOutQty.toInt()}",
                                            "Closing" to "${totalClosingStock.toInt()}",
                                            "Valuation" to "₹${String.format("%.2f", totalValuation)}"
                                        )

                                        val config = PDFGenerator.PDFConfig(
                                            title = "Stock Report",
                                            fileName = "Stock_Report",
                                            columns = columns,
                                            data = filteredEntries,
                                            totalRow = totalRow
                                        )

                                        PDFGenerator.generateAndSharePDF(context, config)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366) // WhatsApp green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHARE",
                                    fontWeight = FontWeight.SemiBold
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
                    colors = CardDefaults.cardColors(
                        containerColor = JivaColors.Teal
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stock Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Items: ${filteredEntries.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.White
                            )
                            Text(
                                text = "Total Valuation: ₹${String.format("%.2f", totalValuation)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.White
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
                        Text(
                            text = "Stock Report Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Horizontally scrollable table
                        val tableScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                        ) {
                            // Table Header
                            StockTableHeader()

                            // Table Rows with Loading Animation
                            if (isLoading && filteredEntries.isEmpty()) {
                                // Show loading animation when data is loading
                                repeat(5) {
                                    StockLoadingRow()
                                }
                            } else {
                                filteredEntries.forEach { entry ->
                                    StockTableRow(entry = entry)
                                }
                            }

                            // Total Row
                            StockTotalRow(
                                totalOpeningStock = totalOpeningStock,
                                totalInQty = totalInQty,
                                totalOutQty = totalOutQty,
                                totalClosingStock = totalClosingStock,
                                totalValuation = totalValuation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockTableHeader() {
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
        StockHeaderCell("Item ID", modifier = Modifier.width(80.dp))
        StockHeaderCell("Item Name", modifier = Modifier.width(150.dp))
        StockHeaderCell("Opening", modifier = Modifier.width(80.dp))
        StockHeaderCell("IN Qty", modifier = Modifier.width(80.dp))
        StockHeaderCell("Out Qty", modifier = Modifier.width(80.dp))
        StockHeaderCell("Closing", modifier = Modifier.width(80.dp))
        StockHeaderCell("Avg Rate", modifier = Modifier.width(80.dp))
        StockHeaderCell("Valuation", modifier = Modifier.width(100.dp))
        StockHeaderCell("Type", modifier = Modifier.width(80.dp))
        StockHeaderCell("Company", modifier = Modifier.width(100.dp))
        StockHeaderCell("CGST", modifier = Modifier.width(60.dp))
        StockHeaderCell("SGST", modifier = Modifier.width(60.dp))
        StockHeaderCell("IGST", modifier = Modifier.width(60.dp))
    }
}

@Composable
private fun StockHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun StockTableRow(entry: StockEntry) {
    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockCell(entry.itemId, Modifier.width(80.dp))
            StockCell(entry.itemName, Modifier.width(150.dp))
            StockCell(entry.openingStock, Modifier.width(80.dp))
            StockCell(entry.inQty, Modifier.width(80.dp))
            StockCell(entry.outQty, Modifier.width(80.dp))
            StockCell(entry.closingStock, Modifier.width(80.dp))
            StockCell("₹${entry.avgRate}", Modifier.width(80.dp))
            StockCell("₹${entry.valuation}", Modifier.width(100.dp))
            StockCell(entry.itemType, Modifier.width(80.dp))
            StockCell(entry.companyName, Modifier.width(100.dp))
            StockCell("${entry.cgst}%", Modifier.width(60.dp))
            StockCell("${entry.sgst}%", Modifier.width(60.dp))
            StockCell("${entry.igst}%", Modifier.width(60.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun StockCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun StockTotalRow(
    totalOpeningStock: Double,
    totalInQty: Double,
    totalOutQty: Double,
    totalClosingStock: Double,
    totalValuation: Double
) {
    Row(
        modifier = Modifier
            .background(
                JivaColors.DeepBlue.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(230.dp) // Item ID + Item Name columns
        )
        StockCell("${totalOpeningStock.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalInQty.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalOutQty.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("${totalClosingStock.toInt()}", Modifier.width(80.dp), JivaColors.DeepBlue)
        StockCell("-", Modifier.width(80.dp), JivaColors.DeepBlue) // Avg Rate column
        StockCell(
            text = "₹${String.format("%.2f", totalValuation)}",
            modifier = Modifier.width(100.dp),
            color = JivaColors.Green
        )
        StockCell("-", Modifier.width(80.dp), JivaColors.DeepBlue) // Type column
        StockCell("-", Modifier.width(100.dp), JivaColors.DeepBlue) // Company column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // CGST column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // SGST column
        StockCell("-", Modifier.width(60.dp), JivaColors.DeepBlue) // IGST column
    }
}

// Shimmer effect for loading animation
fun Modifier.stockShimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
private fun StockLoadingRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .stockShimmerEffect(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Data placeholders for stock columns
        repeat(13) { index ->
            val width = when (index) {
                0 -> 80.dp   // Item ID
                1 -> 150.dp  // Item Name
                2 -> 80.dp   // Opening
                3 -> 80.dp   // IN Qty
                4 -> 80.dp   // Out Qty
                5 -> 80.dp   // Closing
                6 -> 80.dp   // Avg Rate
                7 -> 100.dp  // Valuation
                8 -> 80.dp   // Type
                9 -> 100.dp  // Company
                10 -> 60.dp  // CGST
                11 -> 60.dp  // SGST
                12 -> 60.dp  // IGST
                else -> 80.dp
            }
            Box(
                modifier = Modifier
                    .width(width)
                    .height(16.dp)
                    .background(JivaColors.LightGray, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun StockReportScreenPreview() {
    StockReportScreenImpl()
}
