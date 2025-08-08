package com.example.jiva.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader



// Data model for Outstanding Report entries
data class OutstandingEntry(
    val acId: String,
    val accountName: String,
    val mobile: String,
    val opening: Double,
    val cr: Double,
    val dr: Double,
    val closingBalance: Double,
    val area: String,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreenImpl(onBackClick: () -> Unit = {}) {
    // State management
    var outstandingOf by remember { mutableStateOf("Customer") }
    var viewAll by remember { mutableStateOf(false) }
    var interestRate by remember { mutableStateOf("0.06") }
    var partyNameSearch by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("All") }
    var isOutstandingDropdownExpanded by remember { mutableStateOf(false) }
    var isAreaDropdownExpanded by remember { mutableStateOf(false) }

    // Selection state for checkboxes
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }

    // Dummy data with more entries for better search testing
    val allEntries = remember {
        listOf(
            OutstandingEntry("001", "ABC Traders", "9876543210", 5000.0, 2000.0, 1500.0, 5500.0, "North", "123 Main St"),
            OutstandingEntry("002", "XYZ Suppliers", "9876543211", 3000.0, 1000.0, 500.0, 3500.0, "South", "456 Oak Ave"),
            OutstandingEntry("003", "PQR Industries", "9876543212", 8000.0, 3000.0, 2000.0, 9000.0, "East", "789 Pine Rd"),
            OutstandingEntry("004", "LMN Corporation", "9876543213", 2000.0, 500.0, 1000.0, 1500.0, "West", "321 Elm St"),
            OutstandingEntry("005", "DEF Enterprises", "9876543214", 6000.0, 2500.0, 1000.0, 7500.0, "North", "654 Maple Dr"),
            OutstandingEntry("006", "GHI Solutions", "9876543215", 4000.0, 1500.0, 2000.0, 3500.0, "South", "987 Cedar Ln"),
            OutstandingEntry("025", "Aman Shaikh", "9876543216", 11000.0, 400.0, 2400.0, 13000.0, "North", "789 Business St"),
            OutstandingEntry("007", "Tech Solutions", "9876543217", 7500.0, 1200.0, 800.0, 8300.0, "East", "456 Tech Park"),
            OutstandingEntry("008", "Medical Supplies", "9876543218", 4500.0, 800.0, 1200.0, 4300.0, "West", "321 Health Ave"),
            OutstandingEntry("009", "Food Distributors", "9876543219", 6200.0, 1500.0, 900.0, 6700.0, "South", "654 Food St")
        )
    }

    // Filtered entries based on search and area
    val filteredEntries = remember(partyNameSearch, selectedArea, allEntries) {
        allEntries.filter { entry ->
            // Party name search - searches in both account name and account ID
            val matchesSearch = if (partyNameSearch.isBlank()) true
                else entry.accountName.contains(partyNameSearch, ignoreCase = true) ||
                     entry.acId.contains(partyNameSearch, ignoreCase = true)

            // Area filter
            val matchesArea = if (selectedArea == "All") true
                else entry.area == selectedArea

            matchesSearch && matchesArea
        }
    }

    // Handle select all functionality
    LaunchedEffect(selectAll, filteredEntries) {
        if (selectAll) {
            selectedEntries = filteredEntries.map { it.acId }.toSet()
        } else {
            selectedEntries = emptySet()
        }
    }

    // Update selectAll state based on individual selections
    LaunchedEffect(selectedEntries, filteredEntries) {
        selectAll = filteredEntries.isNotEmpty() && selectedEntries.containsAll(filteredEntries.map { it.acId })
    }

    // Calculate totals
    val totalBalance = filteredEntries.sumOf { it.closingBalance }
    val totalOpening = filteredEntries.sumOf { it.opening }
    val totalCr = filteredEntries.sumOf { it.cr }
    val totalDr = filteredEntries.sumOf { it.dr }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header
        ResponsiveReportHeader(
            title = "Outstanding Report",
            subtitle = "Manage outstanding payments and dues",
            onBackClick = onBackClick
        )

        // Main content with performance optimizations
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
            // Control Panel Card
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
                        // First row: Outstanding Of dropdown and View All checkbox
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Outstanding Of dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Outstanding Of",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isOutstandingDropdownExpanded,
                                    onExpandedChange = { isOutstandingDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = outstandingOf,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOutstandingDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isOutstandingDropdownExpanded,
                                        onDismissRequest = { isOutstandingDropdownExpanded = false }
                                    ) {
                                        listOf("Customer", "Supplier").forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    outstandingOf = option
                                                    isOutstandingDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // View All checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 24.dp)
                            ) {
                                Checkbox(
                                    checked = viewAll,
                                    onCheckedChange = { viewAll = it },
                                    colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                                )
                                Text(
                                    text = "View All",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue
                                )
                            }
                        }

                        // Interest Calculation Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Interest Calculation",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JivaColors.DeepBlue
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = interestRate,
                                        onValueChange = { interestRate = it },
                                        label = { Text("Interest Rate") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Text(
                                        text = "/Day",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue
                                    )

                                    Button(
                                        onClick = { /* TODO: Calculate interest */ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = JivaColors.Green
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Calculate",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Search and Filter Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Party Name Search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Party Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = partyNameSearch,
                                    onValueChange = { partyNameSearch = it },
                                    placeholder = { Text("Search by name or ID...") },
                                    trailingIcon = {
                                        IconButton(onClick = { partyNameSearch = "" }) {
                                            Icon(
                                                imageVector = if (partyNameSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                                                contentDescription = if (partyNameSearch.isNotEmpty()) "Clear" else "Search"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // Area dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Area",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isAreaDropdownExpanded,
                                    onExpandedChange = { isAreaDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedArea,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAreaDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isAreaDropdownExpanded,
                                        onDismissRequest = { isAreaDropdownExpanded = false }
                                    ) {
                                        listOf("All", "North", "South", "East", "West").forEach { area ->
                                            DropdownMenuItem(
                                                text = { Text(area) },
                                                onClick = {
                                                    selectedArea = area
                                                    isAreaDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Selection info and action buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Selection summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected: ${selectedEntries.size} of ${filteredEntries.size} entries",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectAll,
                                        onCheckedChange = { selectAll = it },
                                        colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                                    )
                                    Text(
                                        text = "Select All",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue
                                    )
                                }
                            }

                            // Action button
                            Button(
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) {
                                        // TODO: Send WhatsApp to selected entries
                                        val selectedData = filteredEntries.filter { it.acId in selectedEntries }
                                        // Handle WhatsApp sending logic here
                                    }
                                },
                                enabled = selectedEntries.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Green,
                                    disabledContainerColor = JivaColors.Green.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send WhatsApp (${selectedEntries.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    // Single Share Button
                    Button(
                        onClick = { /* TODO: Implement share functionality */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp green
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = JivaColors.White
                            )
                            Text(
                                text = "SHARE REPORT",
                                color = JivaColors.White,
                                fontWeight = FontWeight.Medium
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
                            text = "Outstanding Report Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Create shared scroll state for the entire table
                        val tableScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier.horizontalScroll(tableScrollState)
                        ) {
                            // Table Header
                            OutstandingTableHeader(scrollState = tableScrollState)

                            // Table Data
                            filteredEntries.forEach { entry ->
                                OutstandingTableRow(
                                    entry = entry,
                                    isSelected = selectedEntries.contains(entry.acId),
                                    onSelectionChange = { isSelected ->
                                        selectedEntries = if (isSelected) {
                                            selectedEntries + entry.acId
                                        } else {
                                            selectedEntries - entry.acId
                                        }
                                    },
                                    scrollState = tableScrollState
                                )
                            }

                            // Total Row
                            OutstandingTotalRow(
                                totalOpening = totalOpening,
                                totalCr = totalCr,
                                totalDr = totalDr,
                                totalBalance = totalBalance,
                                scrollState = tableScrollState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutstandingTableHeader(scrollState: androidx.compose.foundation.ScrollState) {
    Row(
        modifier = Modifier
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox column header - Fixed width
        Box(
            modifier = Modifier.width(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Select",
                tint = JivaColors.DeepBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        TableHeaderCell("AC ID", modifier = Modifier.width(80.dp))
        TableHeaderCell("Account Name", modifier = Modifier.width(150.dp))
        TableHeaderCell("Mobile", modifier = Modifier.width(120.dp))
        TableHeaderCell("Opening", modifier = Modifier.width(100.dp))
        TableHeaderCell("CR", modifier = Modifier.width(100.dp))
        TableHeaderCell("DR", modifier = Modifier.width(100.dp))
        TableHeaderCell("Closing", modifier = Modifier.width(120.dp))
        TableHeaderCell("Area", modifier = Modifier.width(100.dp))
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun OutstandingTableRow(
    entry: OutstandingEntry,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox column - Fixed width
            Box(
                modifier = Modifier.width(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple),
                    modifier = Modifier.size(20.dp)
                )
            }
            TableCell(entry.acId, modifier = Modifier.width(80.dp))
            TableCell(entry.accountName, modifier = Modifier.width(150.dp))
            TableCell(entry.mobile, modifier = Modifier.width(120.dp))
            TableCell("₹${String.format("%.0f", entry.opening)}", modifier = Modifier.width(100.dp))
            TableCell("₹${String.format("%.0f", entry.cr)}", modifier = Modifier.width(100.dp))
            TableCell("₹${String.format("%.0f", entry.dr)}", modifier = Modifier.width(100.dp))
            TableCell(
                text = "₹${String.format("%.0f", entry.closingBalance)}",
                modifier = Modifier.width(120.dp),
                color = if (entry.closingBalance >= 0) JivaColors.Green else JivaColors.Red
            )
            TableCell(entry.area, modifier = Modifier.width(100.dp))
        }

        Divider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun OutstandingTotalRow(
    totalOpening: Double,
    totalCr: Double,
    totalDr: Double,
    totalBalance: Double,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JivaColors.DeepBlue.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Empty space for checkbox column
        Spacer(modifier = Modifier.weight(0.5f))
        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(3.8f)
        )
        TableCell("₹${String.format("%.0f", totalOpening)}", modifier = Modifier.weight(1f), color = JivaColors.DeepBlue)
        TableCell("₹${String.format("%.0f", totalCr)}", modifier = Modifier.weight(1f), color = JivaColors.DeepBlue)
        TableCell("₹${String.format("%.0f", totalDr)}", modifier = Modifier.weight(1f), color = JivaColors.DeepBlue)
        TableCell(
            text = "₹${String.format("%.0f", totalBalance)}",
            modifier = Modifier.weight(1.8f),
            color = if (totalBalance >= 0) JivaColors.Green else JivaColors.Red
        )
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun OutstandingReportScreenPreview() {
    OutstandingReportScreenImpl()
}
