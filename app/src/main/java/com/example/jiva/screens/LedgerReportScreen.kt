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
import com.example.jiva.components.ReportLoading
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

    // Optimized data loading - only from Room DB for better performance
    val ledgerEntities by viewModel.observeLedger(year).collectAsState(initial = emptyList())

    // Use only Ledger DB data for better performance and stability
    val allEntries = remember(ledgerEntities) {
        try {
            ledgerEntities.map { entity ->
                LedgerEntry(
                    entryNo = entity.entryNo?.toString() ?: "",
                    manualNo = entity.manualNo ?: "",
                    srNo = entity.srNo?.toString() ?: "",
                    entryType = entity.entryType ?: "",
                    entryDate = entity.entryDate?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    refNo = entity.refNo ?: "",
                    acId = entity.acId?.toString() ?: "",
                    dr = entity.dr.toString(),
                    cr = entity.cr.toString(),
                    narration = entity.narration ?: "",
                    isClere = if (entity.isClere) "True" else "False",
                    trascType = entity.trascType ?: "",
                    gstRate = entity.gstRate.toString(),
                    amt = entity.amt.toString(),
                    igst = entity.igst.toString()
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error mapping ledger entities")
            emptyList()
        }
    }

    // Optimized filtering with error handling
    val filteredEntries = remember(entryTypeFilter, dateFromSearch, dateToSearch, narrationSearch, allEntries) {
        try {
            if (allEntries.isEmpty()) {
                emptyList()
            } else {
                allEntries.filter { entry ->
                    try {
                        // Entry Type Filter
                        val entryTypeMatch = when (entryTypeFilter) {
                            "All Types" -> true
                            else -> entry.entryType.equals(entryTypeFilter, ignoreCase = true)
                        }

                        // Date Range Filter (simplified for now)
                        val dateMatch = true // TODO: Implement date filtering

                        // Narration Search Filter
                        val narrationMatch = if (narrationSearch.isBlank()) true else
                            entry.narration.contains(narrationSearch, ignoreCase = true)

                        entryTypeMatch && dateMatch && narrationMatch
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error filtering entry: ${entry.entryNo}")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error during filtering")
            emptyList()
        }
    }

    // Calculate totals from string values
    val totalDr = remember(filteredEntries) {
        filteredEntries.sumOf { it.dr.toDoubleOrNull() ?: 0.0 }
    }
    val totalCr = remember(filteredEntries) {
        filteredEntries.sumOf { it.cr.toDoubleOrNull() ?: 0.0 }
    }
    val totalAmt = remember(filteredEntries) {
        filteredEntries.sumOf { it.amt.toDoubleOrNull() ?: 0.0 }
    }

    // Unified loading screen (matches Outstanding)
    if (isScreenLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ReportLoading(
                title = "Loading Ledger Report...",
                message = loadingMessage,
                progressPercent = loadingProgress
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Responsive Header with Refresh Button
        ResponsiveReportHeader(
            title = "Ledger Report",
            subtitle = "Ledger entries and transactions",
            onBackClick = onBackClick,
            actions = {
                // Refresh Button
                IconButton(
                    onClick = {
                        if (finalUserId != null && !isRefreshing) {
                            scope.launch {
                                isRefreshing = true
                                try {
                                    val result = application.repository.syncLedger(finalUserId, year)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Ledger data refreshed successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to refresh data", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        }
                    }
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
                            contentDescription = "Refresh",
                            tint = JivaColors.White
                        )
                    }
                }
            }
        )

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

                        // Entry Type Filter
                        Column {
                            Text(
                                text = "Entry Type",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = isEntryTypeDropdownExpanded,
                                onExpandedChange = { isEntryTypeDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = entryTypeFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEntryTypeDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isEntryTypeDropdownExpanded,
                                    onDismissRequest = { isEntryTypeDropdownExpanded = false }
                                ) {
                                    listOf("All Types", "Cash Sale", "Credit Sale", "Purchase", "Payment", "Receipt").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                entryTypeFilter = type
                                                isEntryTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Narration Search
                        Column {
                            Text(
                                text = "Search in Narration",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = narrationSearch,
                                onValueChange = { narrationSearch = it },
                                placeholder = { Text("Search in descriptions...") },
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
                                text = "Total Debit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalDr)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Red
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Credit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalCr)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.Green
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Entries",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                            Text(
                                text = "${filteredEntries.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
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
                            text = "Ledger Entries (${filteredEntries.size} entries)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Outstanding Report style table with horizontal scrolling
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header in Outstanding Report style
                            LedgerTableHeader()

                            // Data rows in Outstanding Report style
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredEntries) { entry ->
                                    LedgerTableRow(entry = entry)
                                }

                                // Total row like Outstanding Report
                                item {
                                    LedgerTotalRow(
                                        totalDr = totalDr,
                                        totalCr = totalCr,
                                        totalEntries = filteredEntries.size
                                    )
                                }
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
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerHeaderCell("Entry No", Modifier.width(80.dp))
        LedgerHeaderCell("Manual No", Modifier.width(80.dp))
        LedgerHeaderCell("Type", Modifier.width(100.dp))
        LedgerHeaderCell("Date", Modifier.width(100.dp))
        LedgerHeaderCell("Ref No", Modifier.width(80.dp))
        LedgerHeaderCell("AC ID", Modifier.width(80.dp))
        LedgerHeaderCell("DR", Modifier.width(100.dp))
        LedgerHeaderCell("CR", Modifier.width(100.dp))
        LedgerHeaderCell("Narration", Modifier.width(200.dp))
        LedgerHeaderCell("Clear", Modifier.width(80.dp))
        LedgerHeaderCell("GST Rate", Modifier.width(80.dp))
        LedgerHeaderCell("Amount", Modifier.width(100.dp))
        LedgerHeaderCell("IGST", Modifier.width(80.dp))
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
private fun LedgerTableRow(entry: LedgerEntry) {
    // Safe data processing before rendering (Outstanding Report style)
    val safeEntry = remember(entry) {
        try {
            entry.copy(
                entryNo = entry.entryNo.takeIf { it.isNotBlank() } ?: "N/A",
                manualNo = entry.manualNo.takeIf { it.isNotBlank() } ?: "",
                entryType = entry.entryType.takeIf { it.isNotBlank() } ?: "",
                entryDate = entry.entryDate.takeIf { it.isNotBlank() } ?: "",
                refNo = entry.refNo.takeIf { it.isNotBlank() } ?: "",
                acId = entry.acId.takeIf { it.isNotBlank() } ?: "",
                dr = entry.dr.takeIf { it.isNotBlank() } ?: "0.00",
                cr = entry.cr.takeIf { it.isNotBlank() } ?: "0.00",
                narration = entry.narration.takeIf { it.isNotBlank() } ?: "",
                isClere = entry.isClere.takeIf { it.isNotBlank() } ?: "False",
                gstRate = entry.gstRate.takeIf { it.isNotBlank() } ?: "0.00",
                amt = entry.amt.takeIf { it.isNotBlank() } ?: "0.00",
                igst = entry.igst.takeIf { it.isNotBlank() } ?: "0.00"
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error processing entry: ${entry.entryNo}")
            LedgerEntry("Error", "Error loading data", "", "", "", "", "", "", "", "", "", "", "", "", "")
        }
    }

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerCell(safeEntry.entryNo, Modifier.width(80.dp), JivaColors.DeepBlue)
            LedgerCell(safeEntry.manualNo, Modifier.width(80.dp))
            LedgerCell(safeEntry.entryType, Modifier.width(100.dp), JivaColors.Purple)
            LedgerCell(safeEntry.entryDate, Modifier.width(100.dp))
            LedgerCell(safeEntry.refNo, Modifier.width(80.dp))
            LedgerCell(safeEntry.acId, Modifier.width(80.dp), JivaColors.DeepBlue)
            LedgerCell("₹${safeEntry.dr}", Modifier.width(100.dp), JivaColors.Red)
            LedgerCell("₹${safeEntry.cr}", Modifier.width(100.dp), JivaColors.Green)
            LedgerCell(safeEntry.narration, Modifier.width(200.dp))
            LedgerCell(safeEntry.isClere, Modifier.width(80.dp))
            LedgerCell("${safeEntry.gstRate}%", Modifier.width(80.dp))
            LedgerCell("₹${safeEntry.amt}", Modifier.width(100.dp))
            LedgerCell("₹${safeEntry.igst}", Modifier.width(80.dp))
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
private fun LedgerTotalRow(totalDr: Double, totalCr: Double, totalEntries: Int) {
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
            repeat(6) {
                Box(modifier = Modifier.width(80.dp))
            }

            // Total DR cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalDr)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Total CR cell
            Text(
                text = "Total: ₹${String.format("%.2f", totalCr)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.Green,
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
                modifier = Modifier.width(200.dp)
            )

            // Empty cells for remaining columns
            repeat(4) {
                Box(modifier = Modifier.width(80.dp))
            }
        }
    }
}
