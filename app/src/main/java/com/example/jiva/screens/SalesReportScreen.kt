package com.example.jiva.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

// Data model for Sales/Purchase Report entries
data class SalesReportEntry(
    val trDate: String,
    val partyName: String,
    val gstin: String,
    val entryType: String,
    val refNo: String,
    val itemName: String,
    val hsnNo: String,
    val itemType: String,
    val qty: Double,
    val unit: String,
    val rate: Double,
    val amount: Double,
    val discount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreenImpl(onBackClick: () -> Unit = {}) {
    // State management for input fields
    var startDate by remember { mutableStateOf("01/04/2025") }
    var endDate by remember { mutableStateOf("31/03/2026") }
    var selectedItemType by remember { mutableStateOf("All Types") }
    var selectedReportFor by remember { mutableStateOf("Sale") }
    var partyName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf("All Types") }
    var selectedExempted by remember { mutableStateOf("All") }
    var hsnNo by remember { mutableStateOf("") }
    
    // Dropdown states
    var isItemTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isReportForDropdownExpanded by remember { mutableStateOf(false) }
    var isCompanyDropdownExpanded by remember { mutableStateOf(false) }
    var isExemptedDropdownExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Dummy data for sales/purchase entries
    val allSalesEntries = remember {
        listOf(
            SalesReportEntry("05/08/2025", "Aman Shaikh", "-", "Credit Sale", "1", "Rogar 100ml", "3808", "Fertilizers", 12.0, "Nos", 200.0, 2285.71, 0.0),
            SalesReportEntry("06/08/2025", "ABC Traders", "27ABCDE1234F1Z5", "Cash Sale", "2", "Roundup Herbicide", "3808", "Pesticides", 5.0, "Ltr", 450.0, 2250.0, 100.0),
            SalesReportEntry("07/08/2025", "XYZ Suppliers", "29XYZAB5678G2H6", "Credit Sale", "3", "NPK Fertilizer", "3104", "Fertilizers", 25.0, "Kg", 85.75, 2143.75, 50.0),
            SalesReportEntry("08/08/2025", "PQR Industries", "-", "Cash Sale", "4", "Growth Booster", "3808", "PGR", 8.0, "Nos", 275.0, 2200.0, 0.0),
            SalesReportEntry("09/08/2025", "LMN Corporation", "24LMNOP9012I3J4", "Credit Sale", "5", "Hybrid Tomato Seeds", "1209", "Seeds", 100.0, "Pkt", 15.50, 1550.0, 25.0),
            SalesReportEntry("10/08/2025", "DEF Enterprises", "19DEFGH3456K4L5", "Cash Sale", "6", "Insecticide Spray", "3808", "Pesticides", 15.0, "Ltr", 320.0, 4800.0, 200.0),
            SalesReportEntry("11/08/2025", "GHI Solutions", "-", "Credit Sale", "7", "Organic Manure", "3104", "Fertilizers", 50.0, "Kg", 65.0, 3250.0, 0.0),
            SalesReportEntry("12/08/2025", "Tech Agro", "36TECH7890M5N6", "Cash Sale", "8", "Plant Growth Regulator", "3808", "PGR", 20.0, "Nos", 180.0, 3600.0, 150.0),
            SalesReportEntry("13/08/2025", "Modern Farms", "22MODER4567O7P8", "Credit Sale", "9", "Cotton Seeds", "1209", "Seeds", 75.0, "Pkt", 25.0, 1875.0, 0.0),
            SalesReportEntry("14/08/2025", "Green Valley", "-", "Cash Sale", "10", "Multi-Purpose Cleaner", "3402", "General", 30.0, "Nos", 45.0, 1350.0, 50.0)
        )
    }

    // Filter options
    val itemTypeOptions = listOf("All Types", "General", "Pesticides", "Fertilizers", "PGR", "Seeds")
    val reportForOptions = listOf("Sale", "Purchase")
    val companyOptions = listOf("All Types", "Bayer Corp", "Monsanto", "IFFCO", "UPL Limited", "Mahyco", "Syngenta", "Coromandel", "Dhanuka", "Rasi Seeds", "Henkel")
    val exemptedOptions = listOf("All", "Yes", "No")

    // Filtered entries based on search criteria
    val filteredEntries = remember(startDate, endDate, selectedItemType, selectedReportFor, partyName, itemName, selectedCompany, selectedExempted, hsnNo, allSalesEntries) {
        allSalesEntries.filter { entry ->
            val matchesItemType = if (selectedItemType == "All Types") true else entry.itemType == selectedItemType
            val matchesReportFor = entry.entryType.contains(selectedReportFor, ignoreCase = true)
            val matchesPartyName = if (partyName.isBlank()) true else entry.partyName.contains(partyName, ignoreCase = true)
            val matchesItemName = if (itemName.isBlank()) true else entry.itemName.contains(itemName, ignoreCase = true)
            val matchesCompany = selectedCompany == "All Types" // In real app, would match against company field
            val matchesExempted = selectedExempted == "All" // In real app, would check exemption status
            val matchesHsn = if (hsnNo.isBlank()) true else entry.hsnNo.contains(hsnNo)
            
            matchesItemType && matchesReportFor && matchesPartyName && matchesItemName && matchesCompany && matchesExempted && matchesHsn
        }
    }

    // Calculate totals
    val totalQty = filteredEntries.sumOf { it.qty }
    val totalAmount = filteredEntries.sumOf { it.amount }
    val totalDiscount = filteredEntries.sumOf { it.discount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Header with gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(JivaColors.DeepBlue, JivaColors.Purple)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(
                                JivaColors.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = JivaColors.White
                        )
                    }
                    
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "JIVA Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Sales & Purchase Report",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White
                        )
                        Text(
                            text = "Detailed transaction reports and analysis",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Print button
                Button(
                    onClick = { /* TODO: Implement print */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JivaColors.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Print",
                        tint = JivaColors.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRINT",
                        color = JivaColors.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Main content
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
                            text = "Filter Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Date Range Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Date Range",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JivaColors.DeepBlue
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start Date
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Start Date",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { startDate = it },
                                        placeholder = { Text("DD/MM/YYYY") },
                                        trailingIcon = {
                                            IconButton(onClick = { showStartDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Select start date",
                                                    tint = JivaColors.Purple
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                // End Date
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "End Date",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = endDate,
                                        onValueChange = { endDate = it },
                                        placeholder = { Text("DD/MM/YYYY") },
                                        trailingIcon = {
                                            IconButton(onClick = { showEndDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Select end date",
                                                    tint = JivaColors.Purple
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // First row: Item Type, Report For
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item Type Dropdown
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

                            // Report For Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Report For",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isReportForDropdownExpanded,
                                    onExpandedChange = { isReportForDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedReportFor,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isReportForDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isReportForDropdownExpanded,
                                        onDismissRequest = { isReportForDropdownExpanded = false }
                                    ) {
                                        reportForOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedReportFor = option
                                                    isReportForDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Second row: Party Name, Item Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Party Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = partyName,
                                    onValueChange = { partyName = it },
                                    placeholder = { Text("Enter party name") },
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

                        // Third row: Company, Exempted
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Company Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Company",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isCompanyDropdownExpanded,
                                    onExpandedChange = { isCompanyDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCompany,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCompanyDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isCompanyDropdownExpanded,
                                        onDismissRequest = { isCompanyDropdownExpanded = false }
                                    ) {
                                        companyOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedCompany = option
                                                    isCompanyDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Exempted Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exempted",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = isExemptedDropdownExpanded,
                                    onExpandedChange = { isExemptedDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedExempted,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExemptedDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isExemptedDropdownExpanded,
                                        onDismissRequest = { isExemptedDropdownExpanded = false }
                                    ) {
                                        exemptedOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedExempted = option
                                                    isExemptedDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Fourth row: HSN No
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HSN No",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = hsnNo,
                                    onValueChange = { hsnNo = it },
                                    placeholder = { Text("Enter HSN number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            // Empty space to balance the row
                            Spacer(modifier = Modifier.weight(1f))
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
                                onClick = { /* TODO: Print report */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Purple
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Print",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PRINT",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    // Reset all filters
                                    startDate = "01/04/2025"
                                    endDate = "31/03/2026"
                                    selectedItemType = "All Types"
                                    selectedReportFor = "Sale"
                                    partyName = ""
                                    itemName = ""
                                    selectedCompany = "All Types"
                                    selectedExempted = "All"
                                    hsnNo = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Orange
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RESET",
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
                            text = "${selectedReportFor} Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Entries: ${filteredEntries.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.White
                            )
                            Text(
                                text = "Total Amount: ₹${String.format("%.2f", totalAmount)}",
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
                            text = "${selectedReportFor} Report Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Table Header
                        SalesReportTableHeader()

                        // Table Data
                        filteredEntries.forEach { entry ->
                            SalesReportTableRow(entry = entry)
                        }

                        // Total Row
                        SalesReportTotalRow(
                            totalQty = totalQty,
                            totalAmount = totalAmount,
                            totalDiscount = totalDiscount
                        )
                    }
                }
            }


        }
    }
}

@Composable
private fun SalesReportTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SalesReportHeaderCell("Date", modifier = Modifier.weight(0.8f))
        SalesReportHeaderCell("Party", modifier = Modifier.weight(1.2f))
        SalesReportHeaderCell("GSTIN", modifier = Modifier.weight(1f))
        SalesReportHeaderCell("Type", modifier = Modifier.weight(0.8f))
        SalesReportHeaderCell("Ref", modifier = Modifier.weight(0.6f))
        SalesReportHeaderCell("Item", modifier = Modifier.weight(1.5f))
        SalesReportHeaderCell("HSN", modifier = Modifier.weight(0.7f))
        SalesReportHeaderCell("Category", modifier = Modifier.weight(0.8f))
        SalesReportHeaderCell("Qty", modifier = Modifier.weight(0.6f))
        SalesReportHeaderCell("Unit", modifier = Modifier.weight(0.6f))
        SalesReportHeaderCell("Rate", modifier = Modifier.weight(0.8f))
        SalesReportHeaderCell("Amount", modifier = Modifier.weight(1f))
        SalesReportHeaderCell("Discount", modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun SalesReportHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SalesReportTableRow(entry: SalesReportEntry) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SalesReportCell(entry.trDate, modifier = Modifier.weight(0.8f))
            SalesReportCell(entry.partyName, modifier = Modifier.weight(1.2f))
            SalesReportCell(entry.gstin, modifier = Modifier.weight(1f))
            SalesReportCell(entry.entryType, modifier = Modifier.weight(0.8f))
            SalesReportCell(entry.refNo, modifier = Modifier.weight(0.6f))
            SalesReportCell(entry.itemName, modifier = Modifier.weight(1.5f))
            SalesReportCell(entry.hsnNo, modifier = Modifier.weight(0.7f))
            SalesReportCell(entry.itemType, modifier = Modifier.weight(0.8f))
            SalesReportCell("${entry.qty.toInt()}", modifier = Modifier.weight(0.6f))
            SalesReportCell(entry.unit, modifier = Modifier.weight(0.6f))
            SalesReportCell("₹${String.format("%.0f", entry.rate)}", modifier = Modifier.weight(0.8f))
            SalesReportCell("₹${String.format("%.2f", entry.amount)}", modifier = Modifier.weight(1f))
            SalesReportCell("₹${String.format("%.0f", entry.discount)}", modifier = Modifier.weight(0.8f))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SalesReportCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    Text(
        text = text,
        fontSize = 9.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SalesReportTotalRow(
    totalQty: Double,
    totalAmount: Double,
    totalDiscount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JivaColors.DeepBlue.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(6.5f)
        )
        SalesReportCell("${totalQty.toInt()}", modifier = Modifier.weight(0.6f), color = JivaColors.DeepBlue)
        Spacer(modifier = Modifier.weight(0.6f)) // Unit column
        Spacer(modifier = Modifier.weight(0.8f)) // Rate column
        SalesReportCell(
            text = "₹${String.format("%.2f", totalAmount)}",
            modifier = Modifier.weight(1f),
            color = JivaColors.Green
        )
        SalesReportCell(
            text = "₹${String.format("%.2f", totalDiscount)}",
            modifier = Modifier.weight(0.8f),
            color = JivaColors.Orange
        )
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun SalesReportScreenPreview() {
    SalesReportScreenImpl()
}
