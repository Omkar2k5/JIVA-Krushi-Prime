package com.example.jiva.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import com.example.jiva.R
import com.example.jiva.components.ResponsiveReportHeader

// Data model for Stock Report entries
data class StockEntry(
    val itemId: String,
    val itemName: String,
    val openingStock: Double,
    val inQty: Double,
    val outQty: Double,
    val closingStock: Double,
    val avgRate: Double,
    val valuation: Double,
    val itemType: String,
    val companyName: String,
    val cgst: Double,
    val sgst: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportScreenImpl(onBackClick: () -> Unit = {}) {
    // State management for input fields
    var itemCode by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var selectedItemType by remember { mutableStateOf("All") }
    var companyName by remember { mutableStateOf("") }
    var packagingSize by remember { mutableStateOf("") }
    var exempted by remember { mutableStateOf(false) }
    
    // Dropdown states
    var isItemTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Dummy data for stock entries
    val allStockEntries = remember {
        listOf(
            StockEntry("ITM001", "Rogar 100ml", 50.0, 20.0, 15.0, 55.0, 125.50, 6902.50, "General", "Bayer Corp", 9.0, 9.0),
            StockEntry("ITM002", "Roundup Herbicide", 30.0, 10.0, 8.0, 32.0, 450.00, 14400.00, "Pesticides", "Monsanto", 18.0, 18.0),
            StockEntry("ITM003", "NPK Fertilizer", 100.0, 50.0, 40.0, 110.0, 85.75, 9432.50, "Fertilizers", "IFFCO", 5.0, 5.0),
            StockEntry("ITM004", "Growth Booster", 25.0, 15.0, 10.0, 30.0, 275.00, 8250.00, "PGR", "UPL Limited", 12.0, 12.0),
            StockEntry("ITM005", "Hybrid Tomato Seeds", 200.0, 100.0, 80.0, 220.0, 15.50, 3410.00, "Seeds", "Mahyco", 5.0, 5.0),
            StockEntry("ITM006", "Insecticide Spray", 40.0, 25.0, 20.0, 45.0, 320.00, 14400.00, "Pesticides", "Syngenta", 18.0, 18.0),
            StockEntry("ITM007", "Organic Manure", 75.0, 30.0, 25.0, 80.0, 65.00, 5200.00, "Fertilizers", "Coromandel", 5.0, 5.0),
            StockEntry("ITM008", "Plant Growth Regulator", 20.0, 12.0, 8.0, 24.0, 180.00, 4320.00, "PGR", "Dhanuka", 12.0, 12.0),
            StockEntry("ITM009", "Cotton Seeds", 150.0, 75.0, 60.0, 165.0, 25.00, 4125.00, "Seeds", "Rasi Seeds", 5.0, 5.0),
            StockEntry("ITM010", "Multi-Purpose Cleaner", 60.0, 20.0, 18.0, 62.0, 45.00, 2790.00, "General", "Henkel", 9.0, 9.0)
        )
    }

    // Item type options
    val itemTypeOptions = listOf("All", "General", "Pesticides", "Fertilizers", "PGR", "Seeds")

    // Filtered entries based on search criteria
    val filteredEntries = remember(itemCode, brandName, itemName, itemDescription, selectedItemType, companyName, packagingSize, allStockEntries) {
        allStockEntries.filter { entry ->
            val matchesItemCode = if (itemCode.isBlank()) true else entry.itemId.contains(itemCode, ignoreCase = true)
            val matchesBrandName = if (brandName.isBlank()) true else entry.companyName.contains(brandName, ignoreCase = true)
            val matchesItemName = if (itemName.isBlank()) true else entry.itemName.contains(itemName, ignoreCase = true)
            val matchesDescription = if (itemDescription.isBlank()) true else entry.itemName.contains(itemDescription, ignoreCase = true)
            val matchesItemType = if (selectedItemType == "All") true else entry.itemType == selectedItemType
            val matchesCompany = if (companyName.isBlank()) true else entry.companyName.contains(companyName, ignoreCase = true)
            
            matchesItemCode && matchesBrandName && matchesItemName && matchesDescription && matchesItemType && matchesCompany
        }
    }

    // Calculate totals
    val totalOpeningStock = filteredEntries.sumOf { it.openingStock }
    val totalInQty = filteredEntries.sumOf { it.inQty }
    val totalOutQty = filteredEntries.sumOf { it.outQty }
    val totalClosingStock = filteredEntries.sumOf { it.closingStock }
    val totalValuation = filteredEntries.sumOf { it.valuation }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header
        ResponsiveReportHeader(
            title = "Stock Report",
            subtitle = "Inventory management and stock analysis",
            onBackClick = onBackClick
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
                                    value = itemCode,
                                    onValueChange = { itemCode = it },
                                    placeholder = { Text("Enter item code") },
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
                                    value = companyName,
                                    onValueChange = { companyName = it },
                                    placeholder = { Text("Enter company name") },
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
                                onClick = { /* TODO: Share report */ },
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

                            // Table Rows
                            filteredEntries.forEach { entry ->
                                StockTableRow(entry = entry)
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
            StockCell("${entry.openingStock.toInt()}", Modifier.width(80.dp))
            StockCell("${entry.inQty.toInt()}", Modifier.width(80.dp))
            StockCell("${entry.outQty.toInt()}", Modifier.width(80.dp))
            StockCell("${entry.closingStock.toInt()}", Modifier.width(80.dp))
            StockCell("₹${String.format("%.2f", entry.avgRate)}", Modifier.width(80.dp))
            StockCell("₹${String.format("%.2f", entry.valuation)}", Modifier.width(100.dp))
            StockCell(entry.itemType, Modifier.width(80.dp))
            StockCell(entry.companyName, Modifier.width(100.dp))
            StockCell("${entry.cgst}%", Modifier.width(60.dp))
            StockCell("${entry.sgst}%", Modifier.width(60.dp))
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
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun StockReportScreenPreview() {
    StockReportScreenImpl()
}
