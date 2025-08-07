package com.example.jiva.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚧",
                        fontSize = 64.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = description,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "This feature is under development and will be available soon.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// Individual screen composables
@Composable
fun OutstandingReportScreen(onBackClick: () -> Unit = {}) {
    OutstandingReportScreenImpl(onBackClick = onBackClick)
}

@Composable
fun LedgerScreen(onBackClick: () -> Unit = {}) {
    LedgerReportScreenImpl(onBackClick = onBackClick)
}

@Composable
fun StockReportScreen(onBackClick: () -> Unit = {}) {
    StockReportScreenImpl(onBackClick = onBackClick)
}

@Composable
fun ItemSellPurchaseScreen(onBackClick: () -> Unit = {}) {
    SalesReportScreenImpl(onBackClick = onBackClick)
}

@Composable
fun DayEndReportScreen(onBackClick: () -> Unit = {}) {
    PlaceholderScreen(
        title = "Day End Report",
        description = "Generate comprehensive daily summary reports including sales, purchases, and closing balances.",
        onBackClick = onBackClick
    )
}

@Composable
fun WhatsAppMarketingScreen(onBackClick: () -> Unit = {}) {
    PlaceholderScreen(
        title = "WhatsApp Bulk Marketing",
        description = "Send bulk marketing messages, promotional offers, and customer communications via WhatsApp.",
        onBackClick = onBackClick
    )
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    MaterialTheme {
        PlaceholderScreen(
            title = "Sample Screen",
            description = "This is a sample placeholder screen for preview purposes."
        )
    }
}
