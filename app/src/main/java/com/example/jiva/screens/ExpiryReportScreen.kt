package com.example.jiva.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.JivaColors
import com.example.jiva.utils.PDFGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ExpiryEntry(
    val itemId: String,
    val itemName: String,
    val itemType: String,
    val batchNumber: String,
    val expiryDate: String,
    val qty: Int,
    val daysToExpiry: Long = 0
)

@Composable
fun ExpiryReportScreen(onBackClick: () -> Unit = {}) {
    ExpiryReportScreenImpl(onBackClick = onBackClick)
}

@Composable
private fun ExpiryReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Filter states
    var showOnlyExpiring by remember { mutableStateOf(true) } // Show only items expiring within 90 days
    var selectedDays by remember { mutableStateOf("90") } // Days filter
    var itemNameSearch by remember { mutableStateOf("") }

    // Current date for calculations
    val currentDate = remember { Date() }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Dummy data for expiry entries
    val allEntries = remember {
        val entries = listOf(
            ExpiryEntry("ITM001", "Rice Basmati Premium", "Food Grain", "RB2024001", "15/02/2025", 50),
            ExpiryEntry("ITM002", "Wheat Flour 1kg", "Food Grain", "WF2024002", "28/01/2025", 75),
            ExpiryEntry("ITM003", "Sugar White Crystal", "Sweetener", "SW2024003", "10/03/2025", 100),
            ExpiryEntry("ITM004", "Cooking Oil Sunflower", "Oil", "CO2024004", "05/02/2025", 25),
            ExpiryEntry("ITM005", "Dal Moong Green", "Pulses", "DM2024005", "20/04/2025", 60),
            ExpiryEntry("ITM006", "Tea Leaves Premium", "Beverage", "TL2024006", "15/06/2025", 40),
            ExpiryEntry("ITM007", "Coffee Powder Instant", "Beverage", "CP2024007", "30/01/2025", 30),
            ExpiryEntry("ITM008", "Milk Powder", "Dairy", "MP2024008", "12/02/2025", 20),
            ExpiryEntry("ITM009", "Turmeric Powder", "Spice", "TP2024009", "25/07/2025", 35),
            ExpiryEntry("ITM010", "Red Chili Powder", "Spice", "RC2024010", "18/02/2025", 45),
            ExpiryEntry("ITM011", "Biscuits Pack", "Snack", "BP2024011", "08/02/2025", 80),
            ExpiryEntry("ITM012", "Bread Loaf", "Bakery", "BL2024012", "25/01/2025", 15),
            ExpiryEntry("ITM013", "Yogurt Cup", "Dairy", "YC2024013", "22/01/2025", 25),
            ExpiryEntry("ITM014", "Cheese Slice", "Dairy", "CS2024014", "02/02/2025", 12),
            ExpiryEntry("ITM015", "Butter Pack", "Dairy", "BT2024015", "28/02/2025", 18),
            ExpiryEntry("ITM016", "Jam Bottle", "Preserve", "JB2024016", "15/08/2025", 22),
            ExpiryEntry("ITM017", "Pickle Jar", "Preserve", "PJ2024017", "10/09/2025", 28),
            ExpiryEntry("ITM018", "Noodles Pack", "Instant Food", "NP2024018", "05/03/2025", 65)
        ).map { entry ->
            // Calculate days to expiry
            val expiryDateParsed = try {
                dateFormat.parse(entry.expiryDate)
            } catch (e: Exception) { null }
            
            val daysToExpiry = if (expiryDateParsed != null) {
                val diffInMillis = expiryDateParsed.time - currentDate.time
                TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
            } else 0L
            
            entry.copy(daysToExpiry = daysToExpiry)
        }
        entries
    }

    // Filtered entries based on search criteria
    val filteredEntries = remember(showOnlyExpiring, selectedDays, itemNameSearch, allEntries) {
        derivedStateOf {
            val daysLimit = selectedDays.toLongOrNull() ?: 90L

            allEntries.filter { entry ->
                val nameMatch = if (itemNameSearch.isNotBlank()) {
                    entry.itemName.contains(itemNameSearch, ignoreCase = true) ||
                    entry.itemId.contains(itemNameSearch, ignoreCase = true)
                } else true

                val expiryMatch = if (showOnlyExpiring) {
                    entry.daysToExpiry <= daysLimit && entry.daysToExpiry >= 0
                } else true

                nameMatch && expiryMatch
            }.sortedBy { it.daysToExpiry } // Sort by days to expiry (most urgent first)
        }
    }.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Header
        ResponsiveReportHeader(
            title = "Expiry Report",
            subtitle = "Items nearing expiration date",
            onBackClick = onBackClick
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
                            text = "Expiry Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Filter options row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item Name search
                            OutlinedTextField(
                                value = itemNameSearch,
                                onValueChange = { itemNameSearch = it },
                                label = { Text("Search Items") },
                                placeholder = { Text("Item name or ID") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Days filter
                            OutlinedTextField(
                                value = selectedDays,
                                onValueChange = { selectedDays = it },
                                label = { Text("Days to Expiry") },
                                placeholder = { Text("90") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Days"
                                    )
                                },
                                modifier = Modifier.weight(0.7f),
                                singleLine = true
                            )
                        }

                        // Show only expiring toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showOnlyExpiring,
                                onCheckedChange = { showOnlyExpiring = it },
                                colors = CheckboxDefaults.colors(checkedColor = JivaColors.Orange)
                            )
                            Text(
                                text = "Show only items expiring within ${selectedDays} days",
                                modifier = Modifier.padding(start = 8.dp),
                                color = JivaColors.DeepBlue
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: Apply filters - already applied automatically */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Orange
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Filter",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FILTER",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val columns = listOf(
                                            PDFGenerator.TableColumn("Item ID", 80f) { (it as ExpiryEntry).itemId },
                                            PDFGenerator.TableColumn("Item Name", 180f) { (it as ExpiryEntry).itemName },
                                            PDFGenerator.TableColumn("Type", 120f) { (it as ExpiryEntry).itemType },
                                            PDFGenerator.TableColumn("Batch", 120f) { (it as ExpiryEntry).batchNumber },
                                            PDFGenerator.TableColumn("Expiry Date", 100f) { (it as ExpiryEntry).expiryDate },
                                            PDFGenerator.TableColumn("Qty", 80f) { "${(it as ExpiryEntry).qty}" },
                                            PDFGenerator.TableColumn("Days Left", 100f) {
                                                val entry = it as ExpiryEntry
                                                when {
                                                    entry.daysToExpiry < 0 -> "EXPIRED"
                                                    entry.daysToExpiry == 0L -> "TODAY"
                                                    else -> "${entry.daysToExpiry} days"
                                                }
                                            }
                                        )

                                        val config = PDFGenerator.PDFConfig(
                                            title = "Expiry Report",
                                            fileName = "Expiry_Report",
                                            columns = columns,
                                            data = filteredEntries
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
                            text = "Expiry Report Data (${filteredEntries.size} items)",
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
                            ExpiryTableHeader()

                            // Table Rows
                            filteredEntries.forEach { entry ->
                                ExpiryTableRow(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiryTableHeader() {
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
        ExpiryHeaderCell("Item ID", Modifier.width(80.dp))
        ExpiryHeaderCell("Item Name", Modifier.width(180.dp))
        ExpiryHeaderCell("Item Type", Modifier.width(120.dp))
        ExpiryHeaderCell("Batch Number", Modifier.width(120.dp))
        ExpiryHeaderCell("Expiry Date", Modifier.width(100.dp))
        ExpiryHeaderCell("Qty", Modifier.width(80.dp))
        ExpiryHeaderCell("Days Left", Modifier.width(100.dp))
    }
}

@Composable
private fun ExpiryHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun ExpiryTableRow(entry: ExpiryEntry) {
    // Determine color based on days to expiry
    val urgencyColor = when {
        entry.daysToExpiry < 0 -> JivaColors.Red // Expired
        entry.daysToExpiry <= 7 -> JivaColors.Red // Critical (1 week)
        entry.daysToExpiry <= 30 -> JivaColors.Orange // Warning (1 month)
        entry.daysToExpiry <= 90 -> Color(0xFFFFA500) // Caution (3 months)
        else -> JivaColors.Green // Safe
    }

    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpiryCell(entry.itemId, Modifier.width(80.dp))
            ExpiryCell(entry.itemName, Modifier.width(180.dp))
            ExpiryCell(entry.itemType, Modifier.width(120.dp))
            ExpiryCell(entry.batchNumber, Modifier.width(120.dp))
            ExpiryCell(entry.expiryDate, Modifier.width(100.dp))
            ExpiryCell("${entry.qty}", Modifier.width(80.dp))
            ExpiryCell(
                text = when {
                    entry.daysToExpiry < 0 -> "EXPIRED"
                    entry.daysToExpiry == 0L -> "TODAY"
                    else -> "${entry.daysToExpiry} days"
                },
                modifier = Modifier.width(100.dp),
                color = urgencyColor
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
private fun ExpiryCell(
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
        fontWeight = if (color != Color(0xFF374151)) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
    )
}
