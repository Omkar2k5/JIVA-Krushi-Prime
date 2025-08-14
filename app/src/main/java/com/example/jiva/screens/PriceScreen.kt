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
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.JivaColors

data class PriceEntry(
    val itemId: String,
    val itemName: String,
    val mrp: Double,
    val creditSaleRate: Double,
    val cashSaleRate: Double,
    val wholesaleRate: Double,
    val maxPurchaseRate: Double
)

@Composable
fun PriceScreen(onBackClick: () -> Unit = {}) {
    PriceScreenImpl(onBackClick = onBackClick)
}

@Composable
private fun PriceScreenImpl(onBackClick: () -> Unit = {}) {
    // Filter states
    var itemName by remember { mutableStateOf("") }
    var itemId by remember { mutableStateOf("") }

    // Dummy data for price entries
    val allEntries = remember {
        listOf(
            PriceEntry("ITM001", "Rice Basmati Premium", 120.0, 115.0, 110.0, 105.0, 95.0),
            PriceEntry("ITM002", "Wheat Flour 1kg", 45.0, 42.0, 40.0, 38.0, 35.0),
            PriceEntry("ITM003", "Sugar White Crystal", 55.0, 52.0, 50.0, 48.0, 45.0),
            PriceEntry("ITM004", "Cooking Oil Sunflower", 180.0, 175.0, 170.0, 165.0, 155.0),
            PriceEntry("ITM005", "Dal Moong Green", 140.0, 135.0, 130.0, 125.0, 115.0),
            PriceEntry("ITM006", "Tea Leaves Premium", 320.0, 310.0, 300.0, 290.0, 270.0),
            PriceEntry("ITM007", "Coffee Powder Instant", 280.0, 270.0, 260.0, 250.0, 230.0),
            PriceEntry("ITM008", "Salt Iodized 1kg", 25.0, 23.0, 22.0, 20.0, 18.0),
            PriceEntry("ITM009", "Turmeric Powder", 85.0, 80.0, 78.0, 75.0, 70.0),
            PriceEntry("ITM010", "Red Chili Powder", 95.0, 90.0, 88.0, 85.0, 80.0),
            PriceEntry("ITM011", "Cumin Seeds Whole", 450.0, 440.0, 430.0, 420.0, 400.0),
            PriceEntry("ITM012", "Coriander Seeds", 180.0, 175.0, 170.0, 165.0, 155.0),
            PriceEntry("ITM013", "Mustard Oil 1L", 160.0, 155.0, 150.0, 145.0, 135.0),
            PriceEntry("ITM014", "Onion Fresh 1kg", 35.0, 32.0, 30.0, 28.0, 25.0),
            PriceEntry("ITM015", "Potato Fresh 1kg", 28.0, 26.0, 25.0, 23.0, 20.0)
        )
    }

    // Filtered entries based on search criteria
    val filteredEntries = remember(itemName, itemId, allEntries) {
        allEntries.filter { entry ->
            val nameMatch = if (itemName.isNotBlank()) {
                entry.itemName.contains(itemName, ignoreCase = true)
            } else true

            val idMatch = if (itemId.isNotBlank()) {
                entry.itemId.contains(itemId, ignoreCase = true)
            } else true

            nameMatch && idMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Header
        ResponsiveReportHeader(
            title = "Price Screen",
            subtitle = "Item pricing and rate management",
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
                            text = "Search Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Search filters row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item Name filter
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item Name") },
                                placeholder = { Text("Search by item name") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Item ID filter
                            OutlinedTextField(
                                value = itemId,
                                onValueChange = { itemId = it },
                                label = { Text("Item ID") },
                                placeholder = { Text("Search by item ID") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Item ID"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
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
                                    containerColor = JivaColors.Green
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEARCH",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { /* TODO: Share price list */ },
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
                            text = "Price List Data (${filteredEntries.size} items)",
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
                            PriceTableHeader()

                            // Table Rows
                            filteredEntries.forEach { entry ->
                                PriceTableRow(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceTableHeader() {
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
        PriceHeaderCell("Item ID", Modifier.width(80.dp))
        PriceHeaderCell("Item Name", Modifier.width(180.dp))
        PriceHeaderCell("MRP", Modifier.width(100.dp))
        PriceHeaderCell("Credit Sale Rate", Modifier.width(120.dp))
        PriceHeaderCell("Cash Sale Rate", Modifier.width(120.dp))
        PriceHeaderCell("Wholesale Rate", Modifier.width(120.dp))
        PriceHeaderCell("Max Purchase Rate", Modifier.width(140.dp))
    }
}

@Composable
private fun PriceHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun PriceTableRow(entry: PriceEntry) {
    Column {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriceCell(entry.itemId, Modifier.width(80.dp))
            PriceCell(entry.itemName, Modifier.width(180.dp))
            PriceCell("₹${String.format("%.2f", entry.mrp)}", Modifier.width(100.dp), JivaColors.Purple)
            PriceCell("₹${String.format("%.2f", entry.creditSaleRate)}", Modifier.width(120.dp), JivaColors.Orange)
            PriceCell("₹${String.format("%.2f", entry.cashSaleRate)}", Modifier.width(120.dp), JivaColors.Green)
            PriceCell("₹${String.format("%.2f", entry.wholesaleRate)}", Modifier.width(120.dp), JivaColors.DeepBlue)
            PriceCell("₹${String.format("%.2f", entry.maxPurchaseRate)}", Modifier.width(140.dp), JivaColors.Red)
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PriceCell(
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
