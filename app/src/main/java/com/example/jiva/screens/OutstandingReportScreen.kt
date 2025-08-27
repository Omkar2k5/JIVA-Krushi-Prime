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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.viewmodel.OutstandingReportViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Data model for Outstanding Report entries (all Strings to save space)
data class OutstandingEntry(
    val acId: String,
    val accountName: String,
    val mobile: String,
    val under: String,
    val balance: String,
    val lastDate: String,
    val days: String,
    val creditLimitAmount: String,
    val creditLimitDays: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get the application instance to access the repository
    val application = context.applicationContext as JivaApplication
    
    // Create the ViewModel with the repository
    val viewModel: OutstandingReportViewModel = viewModel(
        factory = OutstandingReportViewModel.Factory(application.database)
    )
    
    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()

    // Local loading states for better UX
    var isScreenLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // State management
    var outstandingOf by remember { mutableStateOf("Customer") }
    var viewAll by remember { mutableStateOf(false) }
    var interestRate by remember { mutableStateOf("0.06") }
    var partyNameSearch by remember { mutableStateOf("") }
    var mobileNumberSearch by remember { mutableStateOf("") }
    var isOutstandingDropdownExpanded by remember { mutableStateOf(false) }

    // WhatsApp messaging state
    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // WhatsApp template message
    val whatsappTemplate = "Hello Kurshi Prime"



    // Sync + observe Outstanding table
    val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
    val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Handle initial screen loading
    LaunchedEffect(Unit) {
        // Simulate initial loading time for smooth UX
        kotlinx.coroutines.delay(500)
        isScreenLoading = false
    }

    // Note: Data loading is now handled automatically by AppDataLoader at app startup
    // No manual loading needed here - data is already available

    // Re-read userId after potential initialization
    val finalUserId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()

    // Note: Removed automatic API call on screen load for better performance
    // Data will be loaded from Room DB only, API calls only on manual refresh

    // Optimized data loading - only from Room DB for better performance
    val outstandingEntities by viewModel.observeOutstanding(year).collectAsState(initial = emptyList())

    // Use only Outstanding DB data for better performance and stability
    val allEntries = remember(outstandingEntities) {
        try {
            outstandingEntities.map {
                OutstandingEntry(
                    acId = it.acId,
                    accountName = it.accountName,
                    mobile = it.mobile,
                    under = it.under,
                    balance = it.balance,
                    lastDate = it.lastDate,
                    days = it.days,
                    creditLimitAmount = it.creditLimitAmount,
                    creditLimitDays = it.creditLimitDays
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping outstanding entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(partyNameSearch, mobileNumberSearch, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        val nameMatch = if (partyNameSearch.isBlank()) true else
                            entry.accountName.contains(partyNameSearch, ignoreCase = true)
                        val mobileMatch = if (mobileNumberSearch.isBlank()) true else
                            entry.mobile.contains(mobileNumberSearch, ignoreCase = true)
                        nameMatch && mobileMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry: ${entry.acId}")
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
            selectedEntries = filteredEntries.map { it.acId }.toSet()
        } else {
            selectedEntries = emptySet()
        }
    }

    // Update selectAll state based on individual selections
    LaunchedEffect(selectedEntries, filteredEntries) {
        selectAll = filteredEntries.isNotEmpty() && selectedEntries.containsAll(filteredEntries.map { it.acId })
    }

    // Calculate total balance with proper string to number conversion
    val totalBalance = remember(filteredEntries) {
        try {
            filteredEntries.sumOf { entry ->
                try {
                    // Remove currency symbols, commas, and convert to double
                    val cleanBalance = entry.balance
                        .replace("₹", "")
                        .replace(",", "")
                        .replace(" ", "")
                        .trim()
                    cleanBalance.toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    0.0
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error calculating total balance")
            0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header with Refresh Button
        ResponsiveReportHeader(
            title = "Outstanding Report",
            subtitle = "Manage outstanding payments and dues",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = {
                        if (finalUserId != null) {
                            isRefreshing = true
                            scope.launch {
                                viewModel.refreshOutstandingData(finalUserId, year, context)
                                kotlinx.coroutines.delay(1000) // Show loading for at least 1 second
                                isRefreshing = false
                            }
                        }
                    },
                    enabled = finalUserId != null && !isRefreshing,
                    modifier = Modifier
                        .background(
                            JivaColors.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = JivaColors.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Outstanding Data",
                            tint = JivaColors.White
                        )
                    }
                }
            }
        )

        // Show loading screen overlay during initial load
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
                        modifier = Modifier.size(48.dp),
                        color = JivaColors.DeepBlue,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Loading Outstanding Report...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = JivaColors.DeepBlue
                    )
                }
            }
        } else {
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
                        // Simplified controls: only search by Account Name or Mobile

                        // Removed Interest Calculation Section as per new requirements

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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = JivaColors.DeepBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // Mobile Number search
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mobile Number",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = mobileNumberSearch,
                                    onValueChange = { mobileNumberSearch = it },
                                    placeholder = { Text("Search by mobile...") },
                                    trailingIcon = {
                                        IconButton(onClick = { mobileNumberSearch = "" }) {
                                            Icon(
                                                imageVector = if (mobileNumberSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Phone,
                                                contentDescription = if (mobileNumberSearch.isNotEmpty()) "Clear" else "Mobile"
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = JivaColors.DeepBlue
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Filter Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Clear Filters Button
                            OutlinedButton(
                                onClick = {
                                    partyNameSearch = ""
                                    mobileNumberSearch = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Filters",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Filters")
                            }

                            // Apply Filters Button (for future use)
                            Button(
                                onClick = {
                                    // Filters are applied automatically through remember
                                    // This button can be used for additional filter actions if needed
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = JivaColors.DeepBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Apply Filters",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Filters")
                            }
                        }

                    }
                }
            }



            // WhatsApp Messaging Section
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
                            text = "WhatsApp Message",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Selection summary and controls
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

                        // WhatsApp template preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Message Template:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DarkGray
                                )
                                Text(
                                    text = whatsappTemplate,
                                    fontSize = 14.sp,
                                    color = JivaColors.DeepBlue,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Send WhatsApp button
                        Button(
                            onClick = {
                                if (selectedEntries.isNotEmpty()) {
                                    scope.launch {
                                        sendWhatsAppMessages(context, filteredEntries, selectedEntries, whatsappTemplate)
                                    }
                                }
                            },
                            enabled = selectedEntries.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366), // WhatsApp green
                                disabledContainerColor = JivaColors.LightGray
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
                                text = "Send WhatsApp to ${selectedEntries.size} contacts",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Action Buttons - Width adjusted to match table
            item {
                // Calculate table width: 50+80+180+140+160+120+140+80+140+140+72(spacing) = 1302dp
                val tableWidth = 1302.dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.width(tableWidth),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        // Single Share Button
                        Button(
                            onClick = {
                                scope.launch {
                                    generateAndSharePDF(context, filteredEntries, totalBalance)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JivaColors.Purple
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

                        // Optimized table with LazyColumn for better performance
                        val tableScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                        ) {
                            // Table Header
                            OutstandingTableHeader()

                            // Show loading or data
                            if (isLoading) {
                                // Loading animation
                                repeat(5) {
                                    OutstandingLoadingRow()
                                }
                            } else if (filteredEntries.isNotEmpty()) {
                                // Show all entries (removed 100 limit)
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
                                        }
                                    )
                                }

                                // Total row
                                OutstandingTotalRow(totalBalance = totalBalance)
                            } else {
                                // Empty state
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray)
                                ) {
                                    Text(
                                        text = "No outstanding data found. Click refresh to load data from server.",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 14.sp,
                                        color = JivaColors.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Close the else block for loading screen
    }
}

@Composable
private fun OutstandingTableHeader() {
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
        // Checkbox column header
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
        OutstandingHeaderCell("AC ID", Modifier.width(80.dp))
        OutstandingHeaderCell("Account Name", Modifier.width(180.dp))
        OutstandingHeaderCell("Mobile", Modifier.width(140.dp))
        OutstandingHeaderCell("Under", Modifier.width(160.dp))
        OutstandingHeaderCell("Balance", Modifier.width(120.dp))
        OutstandingHeaderCell("Last Date", Modifier.width(140.dp))
        OutstandingHeaderCell("Days", Modifier.width(80.dp))
        OutstandingHeaderCell("Credit Limit Amt", Modifier.width(140.dp))
        OutstandingHeaderCell("Credit Limit Days", Modifier.width(140.dp))
    }
}

@Composable
private fun OutstandingHeaderCell(text: String, modifier: Modifier = Modifier) {
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
    onSelectionChange: (Boolean) -> Unit
) {
    // Safe data processing before rendering
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                acId = entry.acId.takeIf { it.isNotBlank() } ?: "N/A",
                accountName = entry.accountName.takeIf { it.isNotBlank() } ?: "Unknown",
                mobile = entry.mobile.takeIf { it.isNotBlank() } ?: "",
                under = entry.under.takeIf { it.isNotBlank() } ?: "",
                balance = entry.balance.takeIf { it.isNotBlank() } ?: "0",
                lastDate = entry.lastDate.takeIf { it.isNotBlank() } ?: "",
                days = entry.days.takeIf { it.isNotBlank() } ?: "",
                creditLimitAmount = entry.creditLimitAmount.takeIf { it.isNotBlank() } ?: "",
                creditLimitDays = entry.creditLimitDays.takeIf { it.isNotBlank() } ?: ""
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.acId}")
            OutstandingEntry("Error", "Error loading data", "", "", "0", "", "", "", "")
        }
    }

    // Safe balance parsing
    val balanceValue = remember(safeEntry.balance) {
        try {
            safeEntry.balance.replace(",", "").toDoubleOrNull() ?: 0.0
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
            // Checkbox column
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
            OutstandingCell(safeEntry.acId, Modifier.width(80.dp))
            OutstandingCell(safeEntry.accountName, Modifier.width(180.dp))
            OutstandingCell(safeEntry.mobile, Modifier.width(140.dp))
            OutstandingCell(safeEntry.under, Modifier.width(160.dp))

            OutstandingCell(
                text = "₹${safeEntry.balance}",
                modifier = Modifier.width(120.dp),
                color = if (balanceValue >= 0) JivaColors.Green else JivaColors.Red
            )
            OutstandingCell(safeEntry.lastDate, Modifier.width(140.dp))
            OutstandingCell(safeEntry.days, Modifier.width(80.dp))
            OutstandingCell(safeEntry.creditLimitAmount, Modifier.width(140.dp))
            OutstandingCell(safeEntry.creditLimitDays, Modifier.width(140.dp))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun OutstandingCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    // Safe text processing before rendering
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



// PDF Generation Function
private suspend fun generateAndSharePDF(
    context: Context,
    entries: List<OutstandingEntry>,
    totalBalance: Double
) {
    withContext(Dispatchers.IO) {
        try {
            // Create PDF document
            val pdfDocument = PdfDocument()

            // Paint objects for different text styles
            val titlePaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val cellPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val borderPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // Table dimensions
            val startX = 30f
            val startY = 120f
            val rowHeight = 25f
            val colWidths = floatArrayOf(60f, 140f, 100f, 90f, 100f)
            val totalWidth = colWidths.sum()
            val headers = arrayOf("AC ID", "Account Name", "Mobile", "Balance", "Area")

            // Calculate rows per page (leaving space for header, title, and footer)
            val maxRowsPerPage = 25
            val totalPages = kotlin.math.ceil(entries.size.toDouble() / maxRowsPerPage).toInt().coerceAtLeast(1)

            var entryIndex = 0

            // Generate pages
            for (pageNum in 1..totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw title and date
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Outstanding Report", 297.5f, 50f, titlePaint)
                canvas.drawText("Generated on: $currentDate", 297.5f, 75f, cellPaint)
                canvas.drawText("Page $pageNum of $totalPages", 297.5f, 95f, cellPaint)

                var currentX = startX
                var currentY = startY

                // Draw table headers
                val headerRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                canvas.drawRect(headerRect, Paint().apply { color = android.graphics.Color.LTGRAY; style = Paint.Style.FILL })

                for (i in headers.indices) {
                    val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(headers[i], currentX + 5f, currentY + 15f, headerPaint)
                    currentX += colWidths[i]
                }

                // Draw data rows for this page
                currentY += rowHeight
                val endIndex = kotlin.math.min(entryIndex + maxRowsPerPage, entries.size)

                for (i in entryIndex until endIndex) {
                    val entry = entries[i]
                    currentX = startX
                    val rowData = arrayOf(
                        entry.acId,
                        entry.accountName.take(20), // Truncate long names
                        entry.mobile,
                        "₹${entry.balance}",
                        entry.under.take(12) // Use 'under' as area/category
                    )

                    for (j in rowData.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[j], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        canvas.drawText(rowData[j], currentX + 5f, currentY + 15f, cellPaint)
                        currentX += colWidths[j]
                    }
                    currentY += rowHeight
                }

                entryIndex = endIndex

                // Draw totals row only on the last page
                if (pageNum == totalPages) {
                    currentX = startX
                    val totalRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                    canvas.drawRect(totalRect, Paint().apply { color = android.graphics.Color.CYAN; style = Paint.Style.FILL; alpha = 100 })

                    val totalData = arrayOf(
                        "TOTAL",
                        "",
                        "",
                        "₹${String.format("%.0f", totalBalance)}",
                        ""
                    )

                    for (i in totalData.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        canvas.drawText(totalData[i], currentX + 5f, currentY + 15f, headerPaint)
                        currentX += colWidths[i]
                    }

                    // Add footer
                    canvas.drawText("Total Entries: ${entries.size}", startX, currentY + 50f, cellPaint)
                    canvas.drawText("Generated by JIVA App", startX, currentY + 70f, cellPaint)
                }

                pdfDocument.finishPage(page)
            }

            // Save PDF to external storage
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Outstanding_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(downloadsDir, fileName)

            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()
            pdfDocument.close()

            // Show success message and share
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                sharePDF(context, file)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun sharePDF(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Outstanding Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the Outstanding Report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Outstanding Report")
        context.startActivity(chooser)

    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun OutstandingLoadingRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .shimmerEffect(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox placeholder
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(20.dp)
                .background(JivaColors.LightGray, RoundedCornerShape(4.dp))
        )
        // Data placeholders
        repeat(8) { index ->
            val width = when (index) {
                0 -> 80.dp
                1 -> 180.dp
                2 -> 140.dp
                3 -> 160.dp
                4 -> 120.dp
                5 -> 140.dp
                6 -> 80.dp
                7 -> 140.dp
                else -> 140.dp
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
private fun OutstandingTotalRow(
    totalBalance: Double
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
        // Empty space for checkbox column
        Box(modifier = Modifier.width(50.dp))

        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(560.dp) // AC ID + Account Name + Mobile + Under columns
        )
        OutstandingCell(
            text = "₹${String.format("%.2f", totalBalance)}",
            modifier = Modifier.width(120.dp),
            color = if (totalBalance >= 0) JivaColors.Green else JivaColors.Red
        )
        // Skip Last Date, Days, Credit limits in total row
        repeat(3) {
            Box(modifier = Modifier.width(140.dp))
        }
    }
}

// Shimmer effect for loading animation
fun Modifier.shimmerEffect(): Modifier = composed {
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

// WhatsApp messaging function
private suspend fun sendWhatsAppMessages(
    context: Context,
    entries: List<OutstandingEntry>,
    selectedIds: Set<String>,
    template: String
) {
    try {
        val selectedEntries = entries.filter { it.acId in selectedIds }

        for (entry in selectedEntries) {
            if (entry.mobile.isNotBlank()) {
                val message = "$template\n\nDear ${entry.accountName},\nYour outstanding balance: ₹${entry.balance}"
                val phoneNumber = entry.mobile.replace("+91", "").replace(" ", "")

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/91$phoneNumber?text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                }

                try {
                    context.startActivity(intent)
                    kotlinx.coroutines.delay(2000) // Delay between messages
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to send WhatsApp to ${entry.mobile}")
                }
            }
        }
    } catch (e: Exception) {
        timber.log.Timber.e(e, "Error in bulk WhatsApp messaging")
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun OutstandingReportScreenPreview() {
    OutstandingReportScreenImpl()
}
