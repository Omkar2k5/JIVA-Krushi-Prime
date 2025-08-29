package com.example.jiva.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.jiva.components.ReportLoading
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.R
import com.example.jiva.components.ResponsiveWhatsAppButton
import com.example.jiva.data.repository.JivaRepositoryImpl
import com.example.jiva.viewmodel.AccountSummary
import com.example.jiva.viewmodel.DayEndReportViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
    // Get the context and database
    val context = LocalContext.current
    val database = (context.applicationContext as JivaApplication).database

    // Create repository and view model
    val jivaRepository = remember { JivaRepositoryImpl(database) }
    val viewModel: DayEndReportViewModel = viewModel { DayEndReportViewModel(jivaRepository) }

    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()

    // Currency formatter
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Show toast for error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            android.widget.Toast.makeText(
                context,
                error,
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearError()
        }
    }

    // Prepare WhatsApp message
    val whatsappMessage = remember(uiState) {
        """
        *Day End Report - ${uiState.currentDate}*
        
        Company: ${uiState.companyCode}
        Year: ${uiState.yearString}
        
        Total Accounts: ${uiState.totalAccounts}
        Total Balance: ${currencyFormatter.format(uiState.totalBalance)}
        Accounts Over Credit Limit: ${uiState.accountsOverCreditLimit}
        Inactive Accounts: ${uiState.inactiveAccounts}
        
        Average Balance per Account: ${currencyFormatter.format(uiState.averageBalancePerAccount)}
        Average Days Since Last Transaction: ${uiState.averageDaysSinceLastTransaction.roundToInt()}
        Accounts Within Credit Limit: ${uiState.percentAccountsWithinCreditLimit.roundToInt()}%
        
        Generated via JIVA App
        """.trimIndent()
    }

    // Get status bar height
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top?.toDp() ?: 24.dp
    }

    // Main content with loading state
    if (uiState.isLoading) {
        // Unified loading screen (matches Outstanding)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JivaColors.LightGray),
            contentAlignment = Alignment.Center
        ) {
            ReportLoading(
                title = "Loading Day End Report...",
                message = "Please wait while we prepare your data",
                progressPercent = null
            )
        }
    } else {
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
                    .padding(
                        top = statusBarHeight + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
            ) {
                // Header content: Back button, Logo, Title
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
                            imageVector = Icons.Filled.ArrowBack,
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
                            text = "Company: ${uiState.companyCode} | Year: ${uiState.yearString}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Main content with performance optimizations
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = true
            ) {
                // Summary Cards - First Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Accounts Card
                        SummaryCard(
                            title = "Total Accounts",
                            value = uiState.totalAccounts.toString(),
                            icon = Icons.Default.Person,
                            backgroundColor = JivaColors.Green,
                            modifier = Modifier.weight(1f)
                        )

                        // Total Balance Card
                        SummaryCard(
                            title = "Total Balance",
                            value = currencyFormatter.format(uiState.totalBalance),
                            icon = Icons.Default.AttachMoney,
                            backgroundColor = JivaColors.Orange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Summary Cards - Second Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Over Credit Limit Card
                        SummaryCard(
                            title = "Over Credit Limit",
                            value = uiState.accountsOverCreditLimit.toString(),
                            icon = Icons.Default.Warning,
                            backgroundColor = JivaColors.Purple,
                            modifier = Modifier.weight(1f)
                        )

                        // Inactive Accounts Card
                        SummaryCard(
                            title = "Inactive Accounts",
                            value = uiState.inactiveAccounts.toString(),
                            icon = Icons.Default.DateRange,
                            backgroundColor = JivaColors.Teal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Business Analysis Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Business Analysis",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Average Balance per Account
                            AnalysisProgressBar(
                                label = "Average Balance per Account",
                                value = currencyFormatter.format(uiState.averageBalancePerAccount),
                                progress = 0.7f, // Fixed progress for visual appeal
                                progressColor = JivaColors.Green
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Average Days Since Last Transaction
                            AnalysisProgressBar(
                                label = "Avg. Days Since Last Transaction",
                                value = "${uiState.averageDaysSinceLastTransaction.roundToInt()} days",
                                progress = 0.5f, // Fixed progress for visual appeal
                                progressColor = JivaColors.Orange
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // % Accounts within Credit Limit
                            AnalysisProgressBar(
                                label = "Accounts Within Credit Limit",
                                value = "${uiState.percentAccountsWithinCreditLimit.roundToInt()}%",
                                progress = uiState.percentAccountsWithinCreditLimit.toFloat() / 100f,
                                progressColor = JivaColors.Purple
                            )
                        }
                    }
                }

                // Accounts List Section Header
                item {
                    Text(
                        text = "Accounts List",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JivaColors.DeepBlue,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Accounts List
                items(uiState.accounts) { account ->
                    AccountCard(account = account, currencyFormatter = currencyFormatter)
                }

                // WhatsApp Share Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Share Report",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            ResponsiveWhatsAppButton(
                                onClick = {
                                    // Create intent to share via WhatsApp
                                    val intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, whatsappMessage)
                                        type = "text/plain"
                                        setPackage("com.whatsapp")
                                    }
                                    
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // WhatsApp not installed, show error toast
                                        android.widget.Toast.makeText(
                                            context,
                                            "WhatsApp not installed",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
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

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AnalysisProgressBar(
    label: String,
    value: String,
    progress: Float,
    progressColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = Color.LightGray
        )
    }
}

@Composable
fun AccountCard(
    account: AccountSummary,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (account.isOverCreditLimit) Color.Red else Color.Green)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Account details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.accountName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JivaColors.DeepBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Last Transaction: ${account.lastTransactionDate} (${account.daysSinceLastTransaction} days ago)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Balance
            Text(
                text = currencyFormatter.format(account.balance),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (account.isOverCreditLimit) Color.Red else JivaColors.Green
            )
        }
    }
}