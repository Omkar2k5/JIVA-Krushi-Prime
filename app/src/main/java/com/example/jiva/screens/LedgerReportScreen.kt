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
import com.example.jiva.viewmodel.LedgerReportViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*



/**
 * Ledger Entry data class - Complete with all API fields
 */
data class LedgerEntry(
    val entryNo: String,
    val manualNo: String,
    val srNo: String,
    val entryType: String,
    val entryDate: String,
    val refNo: String,
    val acId: String,
    val dr: String,
    val cr: String,
    val narration: String,
    val isClere: String,
    val trascType: String,
    val gstRate: String,
    val amt: String,
    val igst: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerReportScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication

    // Create the ViewModel with the repository
    val viewModel: LedgerReportViewModel = viewModel(
        factory = LedgerReportViewModel.Factory(application.database)
    )

    // High-performance loading states
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var loadingMessage by remember { mutableStateOf("") }
    var dataLoadingProgress by remember { mutableStateOf(0f) }

    // State management
    var entryTypeFilter by remember { mutableStateOf("All Types") }
    var dateFromSearch by remember { mutableStateOf("") }
    var dateToSearch by remember { mutableStateOf("") }
    var narrationSearch by remember { mutableStateOf("") }
    var isEntryTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Get current year and user ID
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

    // Handle initial screen loading with progress tracking
    LaunchedEffect(Unit) {
        isScreenLoading = true
        loadingMessage = "Initializing Ledger data..."

        // Simulate progressive loading for better UX
        for (i in 0..100 step 10) {
            loadingProgress = i
            dataLoadingProgress = i.toFloat()
            loadingMessage = when {
                i < 30 -> "Loading Ledger data..."
                i < 70 -> "Processing ${i}% complete..."
                i < 100 -> "Finalizing data..."
                else -> "Complete!"
            }
            kotlinx.coroutines.delay(50) // Smooth progress animation
        }

        isScreenLoading = false
    }

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

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
        // Responsive Header
        ResponsiveReportHeader(
            title = "Ledger Report",
            subtitle = "Account transactions and balance",
            onBackClick = onBackClick
        )

        // Create shared scroll state for the entire table
        val tableScrollState = rememberScrollState()

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
                                onClick = {
                                    scope.launch {
                                        val columns = listOf(
                                            PDFGenerator.TableColumn("Date", 100f) { (it as LedgerEntry).entryDate },
                                            PDFGenerator.TableColumn("Type", 80f) { (it as LedgerEntry).entryType },
                                            PDFGenerator.TableColumn("No", 70f) { (it as LedgerEntry).entryNo },
                                            PDFGenerator.TableColumn("Particular", 180f) { (it as LedgerEntry).particular },
                                            PDFGenerator.TableColumn("DR", 100f) { "₹${String.format("%.2f", (it as LedgerEntry).dr)}" },
                                            PDFGenerator.TableColumn("CR", 100f) { "₹${String.format("%.2f", (it as LedgerEntry).cr)}" }
                                        )

                                        val totalRow = mapOf(
                                            "Date" to "TOTAL",
                                            "Type" to "",
                                            "No" to "",
                                            "Particular" to "",
                                            "DR" to "₹${String.format("%.2f", totalDr)}",
                                            "CR" to "₹${String.format("%.2f", totalCr)}"
                                        )

                                        val config = PDFGenerator.PDFConfig(
                                            title = "Ledger Report",
                                            fileName = "Ledger_Report",
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

                        // Horizontally scrollable table
                        val tableScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                        ) {
                            // Table Header
                            LedgerTableHeader()

                            // Table Rows
                            filteredEntries.forEach { entry ->
                                LedgerTableRow(entry = entry, showDetails = showItemDetails)
                            }

                            // Total Row
                            LedgerTotalRow(
                                totalDebit = totalDr,
                                totalCredit = totalCr
                            )
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
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerHeaderCell("Date", Modifier.width(100.dp))
        LedgerHeaderCell("Type", Modifier.width(80.dp))
        LedgerHeaderCell("No", Modifier.width(70.dp))
        LedgerHeaderCell("Particular", Modifier.width(180.dp))
        LedgerHeaderCell("DR", Modifier.width(100.dp))
        LedgerHeaderCell("CR", Modifier.width(100.dp))
        LedgerHeaderCell("Manual", Modifier.width(80.dp))
        LedgerHeaderCell("Details", Modifier.width(150.dp))
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
    // Consistent styling for all rows like other tables
    val textColor = Color(0xFF374151)
    val fontWeight = FontWeight.Normal

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerCell(entry.entryDate, modifier = Modifier.width(100.dp), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.entryType, modifier = Modifier.width(80.dp), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.entryNo, modifier = Modifier.width(70.dp), color = textColor, fontWeight = fontWeight)
            LedgerCell(entry.particular, modifier = Modifier.width(180.dp), color = textColor, fontWeight = fontWeight)
            LedgerCell(
                text = if (entry.dr > 0) "₹${String.format("%.0f", entry.dr)}" else "",
                modifier = Modifier.width(100.dp),
                color = textColor,
                fontWeight = fontWeight
            )
            LedgerCell(
                text = if (entry.cr > 0) "₹${String.format("%.0f", entry.cr)}" else "",
                modifier = Modifier.width(100.dp),
                color = textColor,
                fontWeight = fontWeight
            )
            LedgerCell(entry.manualNo, modifier = Modifier.width(80.dp), color = textColor, fontWeight = fontWeight)
            LedgerCell(
                text = if (showDetails) entry.details else "",
                modifier = Modifier.width(150.dp),
                color = textColor,
                fontWeight = fontWeight
            )
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
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
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun LedgerTotalRow(
    totalDebit: Double,
    totalCredit: Double
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
        // Date column
        LedgerCell("-", Modifier.width(100.dp), JivaColors.DeepBlue)
        // Type column
        LedgerCell("-", Modifier.width(80.dp), JivaColors.DeepBlue)
        // No column
        LedgerCell("-", Modifier.width(70.dp), JivaColors.DeepBlue)
        // Particular column with TOTAL text
        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(180.dp)
        )
        // DR column
        LedgerCell(
            text = "₹${String.format("%.2f", totalDebit)}",
            modifier = Modifier.width(100.dp),
            color = JivaColors.Red
        )
        // CR column
        LedgerCell(
            text = "₹${String.format("%.2f", totalCredit)}",
            modifier = Modifier.width(100.dp),
            color = JivaColors.Green
        )
        // Manual column
        LedgerCell("-", Modifier.width(80.dp), JivaColors.DeepBlue)
        // Details column
        LedgerCell("-", Modifier.width(150.dp), JivaColors.DeepBlue)
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun LedgerReportScreenPreview() {
    LedgerReportScreenImpl()
}
