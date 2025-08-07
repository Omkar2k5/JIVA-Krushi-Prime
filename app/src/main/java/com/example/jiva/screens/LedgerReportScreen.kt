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
import java.text.SimpleDateFormat
import java.util.*



// Data model for Ledger Report entries
data class LedgerEntry(
    val entryDate: String,
    val entryType: String,
    val entryNo: String,
    val particular: String,
    val dr: Double,
    val cr: Double,
    val manualNo: String,
    val details: String,
    val isSpecialRow: Boolean = false // For Opening Balance, Total, Closing Balance
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerReportScreenImpl(onBackClick: () -> Unit = {}) {
    // State management
    var fromDate by remember { mutableStateOf("01/04/2025") }
    var toDate by remember { mutableStateOf("31/03/2026") }
    var showItemDetails by remember { mutableStateOf(false) }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    
    // Date picker states
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    // Dummy data with multiple accounts for search testing
    val allEntries = remember {
        listOf(
            // Account 25 - Aman Shaikh entries
            LedgerEntry("01-Apr-2025", "", "", "Opening Balance - Aman Shaikh (25)", 11000.0, 0.0, "25", "", true),
            LedgerEntry("05-Aug-2025", "Credit Sale", "1", "Cash Amount Credit - Aman Shaikh", 0.0, 400.0, "25", "Account: 25"),
            LedgerEntry("05-Aug-2025", "Credit Sale", "1", "Credit Sale No:1 - Aman Shaikh", 2400.0, 0.0, "25", "Rogar 100ml -12, Account: 25"),
            LedgerEntry("10-Aug-2025", "Payment", "2", "Payment Received - Aman Shaikh", 0.0, 1000.0, "25", "Cash payment, Account: 25"),

            // Account 001 - ABC Traders entries
            LedgerEntry("01-Apr-2025", "", "", "Opening Balance - ABC Traders (001)", 5000.0, 0.0, "001", "", true),
            LedgerEntry("15-Sep-2025", "Credit Sale", "3", "Credit Sale No:3 - ABC Traders", 1500.0, 0.0, "001", "Medicine -5, Account: 001"),
            LedgerEntry("20-Oct-2025", "Payment", "4", "Payment Received - ABC Traders", 0.0, 500.0, "001", "Bank transfer, Account: 001"),

            // Account 002 - XYZ Suppliers entries
            LedgerEntry("01-Apr-2025", "", "", "Opening Balance - XYZ Suppliers (002)", 3000.0, 0.0, "002", "", true),
            LedgerEntry("12-Nov-2025", "Purchase", "5", "Purchase from XYZ Suppliers", 0.0, 2000.0, "002", "Inventory purchase, Account: 002"),
            LedgerEntry("25-Dec-2025", "Payment", "6", "Payment to XYZ Suppliers", 1800.0, 0.0, "002", "Supplier payment, Account: 002"),

            // Totals and closing (these will be calculated dynamically in real implementation)
            LedgerEntry("31-Mar-2026", "", "", "Total", 24700.0, 3900.0, "", "", true),
            LedgerEntry("31-Mar-2026", "", "", "Closing Balance", 20800.0, 0.0, "", "", true)
        )
    }

    // Filtered entries based on date range and account
    val filteredEntries = remember(fromDate, toDate, accountNumber, accountName, allEntries) {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val fromDateParsed = try { 
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fromDate) 
        } catch (e: Exception) { null }
        val toDateParsed = try { 
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(toDate) 
        } catch (e: Exception) { null }

        allEntries.filter { entry ->
            // Always include special rows (Opening Balance, Total, Closing Balance)
            if (entry.isSpecialRow) return@filter true
            
            // Date filter
            val entryDateParsed = try { 
                dateFormat.parse(entry.entryDate) 
            } catch (e: Exception) { null }
            
            val dateInRange = if (fromDateParsed != null && toDateParsed != null && entryDateParsed != null) {
                entryDateParsed >= fromDateParsed && entryDateParsed <= toDateParsed
            } else true
            
            // Account filter - searches in multiple fields for better matching
            val accountMatches = if (accountNumber.isNotBlank() || accountName.isNotBlank()) {
                val numberMatch = if (accountNumber.isNotBlank()) {
                    entry.manualNo.contains(accountNumber, ignoreCase = true) ||
                    entry.particular.contains("($accountNumber)", ignoreCase = true) ||
                    entry.details.contains("Account: $accountNumber", ignoreCase = true)
                } else true

                val nameMatch = if (accountName.isNotBlank()) {
                    entry.particular.contains(accountName, ignoreCase = true) ||
                    entry.details.contains(accountName, ignoreCase = true)
                } else true

                numberMatch && nameMatch
            } else true
            
            dateInRange && accountMatches
        }
    }

    // Calculate totals
    val totalDr = filteredEntries.filter { !it.isSpecialRow }.sumOf { it.dr }
    val totalCr = filteredEntries.filter { !it.isSpecialRow }.sumOf { it.cr }
    val balance = totalDr - totalCr

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
                            text = "Ledger Report",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White
                        )
                        Text(
                            text = "Account transactions and balance",
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
                            text = "Filter Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Date Range Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Date between",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JivaColors.DeepBlue
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // From Date
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "From",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = fromDate,
                                        onValueChange = { fromDate = it },
                                        placeholder = { Text("DD/MM/YYYY") },
                                        trailingIcon = {
                                            IconButton(onClick = { showFromDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Select from date",
                                                    tint = JivaColors.Purple
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                // To Date
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "To",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = toDate,
                                        onValueChange = { toDate = it },
                                        placeholder = { Text("DD/MM/YYYY") },
                                        trailingIcon = {
                                            IconButton(onClick = { showToDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Select to date",
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

                        // Show Item Details Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showItemDetails,
                                onCheckedChange = { showItemDetails = it },
                                colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                            )
                            Text(
                                text = "Show Item Details with Rates",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue
                            )
                        }

                        // Account Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JivaColors.DeepBlue
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Account Number
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Account Number",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = accountNumber,
                                        onValueChange = { accountNumber = it },
                                        placeholder = { Text("Enter account number") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                // Account selection button
                                Button(
                                    onClick = { /* TODO: Open account selection dialog */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JivaColors.Orange
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 24.dp)
                                ) {
                                    Text("...")
                                }
                            }

                            // Account Name
                            Column {
                                Text(
                                    text = "Account Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = accountName,
                                    onValueChange = { accountName = it },
                                    placeholder = { Text("Enter account name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
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
                        }
                    }
                }
            }

            // Balance Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (balance >= 0) JivaColors.Green else JivaColors.Red
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (balance >= 0)
                                "Rs. ${String.format("%.0f", balance)} to be received."
                            else
                                "Rs. ${String.format("%.0f", -balance)} to be paid.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.White,
                            textAlign = TextAlign.Center
                        )
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
                            text = "Ledger Entries",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Table Header
                        LedgerTableHeader()

                        // Table Data
                        filteredEntries.forEach { entry ->
                            LedgerTableRow(entry = entry, showDetails = showItemDetails)
                        }
                    }
                }
            }

            // Summary Footer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = JivaColors.DeepBlue
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.White
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    text = "Debit: ₹${String.format("%.2f", totalDr)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JivaColors.White
                                )
                                Text(
                                    text = "Credit: ₹${String.format("%.2f", totalCr)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JivaColors.White
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
private fun LedgerTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerHeaderCell("Date", modifier = Modifier.weight(1.2f))
        LedgerHeaderCell("Type", modifier = Modifier.weight(1f))
        LedgerHeaderCell("No", modifier = Modifier.weight(0.6f))
        LedgerHeaderCell("Particular", modifier = Modifier.weight(2f))
        LedgerHeaderCell("DR", modifier = Modifier.weight(1f))
        LedgerHeaderCell("CR", modifier = Modifier.weight(1f))
        LedgerHeaderCell("Manual", modifier = Modifier.weight(0.8f))
        LedgerHeaderCell("Details", modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun LedgerHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun LedgerTableRow(
    entry: LedgerEntry,
    showDetails: Boolean
) {
    val backgroundColor = if (entry.isSpecialRow) {
        JivaColors.DeepBlue.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val textColor = if (entry.isSpecialRow) {
        JivaColors.DeepBlue
    } else {
        Color(0xFF374151)
    }

    val fontWeight = if (entry.isSpecialRow) {
        FontWeight.Bold
    } else {
        FontWeight.Normal
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerCell(entry.entryDate, modifier = Modifier.weight(1.2f), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.entryType, modifier = Modifier.weight(1f), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.entryNo, modifier = Modifier.weight(0.6f), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.particular, modifier = Modifier.weight(2f), color = textColor, fontWeight = fontWeight)
            LedgerCell(
                text = if (entry.dr > 0) "₹${String.format("%.0f", entry.dr)}" else "",
                modifier = Modifier.weight(1f),
                color = textColor,
                fontWeight = fontWeight
            )
            LedgerCell(
                text = if (entry.cr > 0) "₹${String.format("%.0f", entry.cr)}" else "",
                modifier = Modifier.weight(1f),
                color = textColor,
                fontWeight = fontWeight
            )
            LedgerCell(entry.manualNo, modifier = Modifier.weight(0.8f), color = textColor, fontWeight = fontWeight)
            LedgerCell(
                text = if (showDetails) entry.details else "",
                modifier = Modifier.weight(1.5f),
                color = textColor,
                fontWeight = fontWeight
            )
        }

        if (!entry.isSpecialRow) {
            Divider(
                color = JivaColors.LightGray,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun LedgerCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151),
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color,
        fontWeight = fontWeight,
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun LedgerReportScreenPreview() {
    LedgerReportScreenImpl()
}
