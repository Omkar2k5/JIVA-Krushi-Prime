package com.example.jiva.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import com.example.jiva.R
import java.text.SimpleDateFormat
import java.util.*

// Data model for Day End Report
data class DayEndData(
    val totalSale: Double,
    val totalPurchase: Double,
    val cashReceived: Double,
    val cashInHand: Double,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEndReportScreenImpl(onBackClick: () -> Unit = {}) {
    // Current date
    val currentDate = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    // Dummy data for day end report
    val dayEndData = remember {
        DayEndData(
            totalSale = 45750.50,
            totalPurchase = 28900.75,
            cashReceived = 32500.00,
            cashInHand = 18650.25,
            date = currentDate
        )
    }

    // Calculate derived values
    val netProfit = dayEndData.totalSale - dayEndData.totalPurchase
    val cashFlow = dayEndData.cashReceived - dayEndData.cashInHand

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
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First row: Back button, Logo, Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Day End Report",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White
                        )
                        Text(
                            text = "Daily business summary - $currentDate",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Second row: WhatsApp button (responsive design)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            // TODO: Send WhatsApp message to self
                            // For now, this would generate a dummy message template
                            val whatsappMessage = generateDayEndWhatsAppMessage(dayEndData, netProfit)
                            // In real implementation, this would open WhatsApp with the message
                            // or send via WhatsApp Business API
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JivaColors.Green
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 120.dp, max = 200.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send WhatsApp",
                                tint = JivaColors.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                color = JivaColors.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Main content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Key Metrics Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Sale Card
                    MetricCardWithDrawable(
                        title = "Total Sale",
                        value = "₹${String.format("%.2f", dayEndData.totalSale)}",
                        iconRes = R.drawable.total_sales,
                        backgroundColor = JivaColors.Green,
                        modifier = Modifier.weight(1f)
                    )

                    // Total Purchase Card
                    MetricCardWithDrawable(
                        title = "Total Purchase",
                        value = "₹${String.format("%.2f", dayEndData.totalPurchase)}",
                        iconRes = R.drawable.totalpurchase,
                        backgroundColor = JivaColors.Orange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cash Received Card
                    MetricCardWithDrawable(
                        title = "Cash Received",
                        value = "₹${String.format("%.2f", dayEndData.cashReceived)}",
                        iconRes = R.drawable.cashrecived,
                        backgroundColor = JivaColors.Teal,
                        modifier = Modifier.weight(1f)
                    )

                    // Cash In Hand Card
                    MetricCardWithDrawable(
                        title = "Cash In Hand",
                        value = "₹${String.format("%.2f", dayEndData.cashInHand)}",
                        iconRes = R.drawable.cashinhand,
                        backgroundColor = JivaColors.Purple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary Analysis Card
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
                            text = "Business Analysis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Net Profit/Loss
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Profit/Loss:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue
                            )
                            Text(
                                text = "₹${String.format("%.2f", netProfit)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit >= 0) JivaColors.Green else JivaColors.Red
                            )
                        }

                        // Cash Flow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cash Flow:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DeepBlue
                            )
                            Text(
                                text = "₹${String.format("%.2f", cashFlow)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cashFlow >= 0) JivaColors.Green else JivaColors.Red
                            )
                        }

                        HorizontalDivider(
                            color = JivaColors.LightGray,
                            thickness = 1.dp
                        )

                        // Performance Indicators
                        Text(
                            text = "Performance Indicators",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JivaColors.DeepBlue
                        )

                        // Sale vs Purchase Ratio
                        val saleRatio = (dayEndData.totalSale / (dayEndData.totalSale + dayEndData.totalPurchase)) * 100
                        PerformanceIndicator(
                            label = "Sale vs Purchase Ratio",
                            percentage = saleRatio,
                            color = JivaColors.Green
                        )

                        // Cash Efficiency
                        val cashEfficiency = (dayEndData.cashInHand / dayEndData.cashReceived) * 100
                        PerformanceIndicator(
                            label = "Cash Efficiency",
                            percentage = cashEfficiency,
                            color = JivaColors.Teal
                        )
                    }
                }
            }

            // Visual Chart Card (Simple Bar Chart Representation)
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
                            text = "Financial Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        // Simple Bar Chart
                        SimpleBarChart(
                            data = listOf(
                                ChartData("Sale", dayEndData.totalSale, JivaColors.Green),
                                ChartData("Purchase", dayEndData.totalPurchase, JivaColors.Orange),
                                ChartData("Cash Received", dayEndData.cashReceived, JivaColors.Teal),
                                ChartData("Cash In Hand", dayEndData.cashInHand, JivaColors.Purple)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = JivaColors.White,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricCardWithDrawable(
    title: String,
    value: String,
    iconRes: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PerformanceIndicator(
    label: String,
    percentage: Double,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = JivaColors.DeepBlue
            )
            Text(
                text = "${String.format("%.1f", percentage)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    JivaColors.LightGray,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100).toFloat().coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(
                        color,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// Data class for chart
data class ChartData(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
private fun SimpleBarChart(
    data: List<ChartData>
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { item ->
            val barWidth = (item.value / maxValue).toFloat()

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = JivaColors.DeepBlue
                    )
                    Text(
                        text = "₹${String.format("%.0f", item.value)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = item.color
                    )
                }

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            JivaColors.LightGray,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = barWidth)
                            .height(24.dp)
                            .background(
                                item.color,
                                RoundedCornerShape(12.dp)
                            )
                    )

                    // Value text on bar
                    Text(
                        text = "₹${String.format("%.0f", item.value)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JivaColors.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// Function to generate WhatsApp message template
private fun generateDayEndWhatsAppMessage(dayEndData: DayEndData, netProfit: Double): String {
    return """
        📊 *JIVA Day End Report* 📊
        📅 Date: ${dayEndData.date}

        💰 *Financial Summary:*
        • Total Sale: ₹${String.format("%.2f", dayEndData.totalSale)}
        • Total Purchase: ₹${String.format("%.2f", dayEndData.totalPurchase)}
        • Cash Received: ₹${String.format("%.2f", dayEndData.cashReceived)}
        • Cash In Hand: ₹${String.format("%.2f", dayEndData.cashInHand)}

        📈 *Performance:*
        • Net Profit: ₹${String.format("%.2f", netProfit)}
        • Status: ${if (netProfit >= 0) "✅ Profitable" else "⚠️ Loss"}

        🎯 *Key Insights:*
        • Sale Performance: ${if (dayEndData.totalSale > 40000) "Excellent" else if (dayEndData.totalSale > 25000) "Good" else "Needs Improvement"}
        • Cash Flow: ${if (dayEndData.cashInHand > 15000) "Healthy" else "Monitor Closely"}

        Generated by JIVA Business Management System

        #DayEndReport #BusinessSummary #JIVA
    """.trimIndent()
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun DayEndReportScreenPreview() {
    DayEndReportScreenImpl()
}
