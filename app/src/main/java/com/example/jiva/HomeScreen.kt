package com.example.jiva

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.data.repository.DummyAuthRepository
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import com.example.jiva.utils.ScreenUtils
import com.example.jiva.utils.AuthUtils
import com.example.jiva.utils.PerformanceUtils
import java.text.SimpleDateFormat
import java.util.*

// Modern vibrant color scheme
object JivaColors {
    val DeepBlue = Color(0xFF1E3A8A)
    val LightBlue = Color(0xFF3B82F6)
    val Purple = Color(0xFF8B5CF6)
    val Pink = Color(0xFFEC4899)
    val Orange = Color(0xFFF59E0B)
    val Green = Color(0xFF10B981)
    val Red = Color(0xFFEF4444)
    val Teal = Color(0xFF14B8A6)
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF8FAFC)
    val CardBackground = Color(0xFFFFFFFF)
}

/**
 * Data class representing a menu item in the home screen
 */
data class MenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val description: String = ""
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    user: User? = null,
    onLogout: () -> Unit = {},
    onNavigateToScreen: (String) -> Unit = {},
    viewModel: HomeViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel: HomeViewModel = viewModel ?: viewModel { HomeViewModel(DummyAuthRepository()) }
    val windowSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity)
    val configuration = LocalConfiguration.current
    val uiState by actualViewModel.uiState.collectAsState()

    // Get user from session if not provided
    val currentUser = user ?: uiState.currentUser

    // Load user session without toast
    LaunchedEffect(Unit) {
        actualViewModel.loadUserSession()
    }

    // Define vibrant menu items with colors
    val menuItems = remember {
        listOf(
            MenuItem(
                id = "outstanding_report",
                title = "Outstanding Report",
                icon = Icons.Default.List,
                color = JivaColors.Purple,
                description = "View outstanding payments and dues"
            ),
            MenuItem(
                id = "ledger",
                title = "Ledger",
                icon = Icons.Default.AccountBox,
                color = JivaColors.Green,
                description = "Account ledger and transactions"
            ),
            MenuItem(
                id = "stock_report",
                title = "Stock Report",
                icon = Icons.Default.Info,
                color = JivaColors.Orange,
                description = "Current stock levels and inventory"
            ),
            MenuItem(
                id = "item_sell_purchase",
                title = "Item Sell/Purchase",
                icon = Icons.Default.ShoppingCart,
                color = JivaColors.Pink,
                description = "Manage sales and purchases"
            ),
            MenuItem(
                id = "day_end_report",
                title = "Day End Report",
                icon = Icons.Default.DateRange,
                color = JivaColors.Teal,
                description = "Daily summary and closing report"
            ),
            MenuItem(
                id = "whatsapp_marketing",
                title = "WhatsApp Bulk Marketing",
                icon = Icons.Default.Send,
                color = JivaColors.Red,
                description = "Send bulk marketing messages"
            ),
            MenuItem(
                id = "price_screen",
                title = "Price Screen",
                icon = Icons.Default.Star,
                color = JivaColors.DeepBlue,
                description = "Item pricing and rate management"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Header with gradient
            ModernHeader(
                currentUser = currentUser,
                onLogout = {
                    actualViewModel.logout()
                    onLogout()
                }
            )

            // Main Content - Responsive Grid with Performance Optimizations
            LazyVerticalGrid(
                columns = GridCells.Fixed(ScreenUtils.getGridColumns()),
                contentPadding = PaddingValues(ScreenUtils.getResponsivePadding()),
                horizontalArrangement = Arrangement.spacedBy(ScreenUtils.getSpacing()),
                verticalArrangement = Arrangement.spacedBy(ScreenUtils.getSpacing()),
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) {
                items(
                    items = menuItems,
                    key = { item -> item.id }
                ) { item ->
                    ModernMenuCard(
                        menuItem = item,
                        onClick = { onNavigateToScreen(item.id) }
                    )
                }
            }

            // Footer Branding
            FooterBranding()
        }
    }
}

@Composable
private fun ModernHeader(
    currentUser: User?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    // Dummy client name - in real app this would come from database
    val clientName = "Tushar Elinje"
    val businessType = "Agricultural Business"

    // Get status bar height
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top?.toDp() ?: 24.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(JivaColors.DeepBlue, JivaColors.LightBlue)
                )
            )
            .padding(
                top = statusBarHeight + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
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
                // App Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Business Logo",
                    modifier = Modifier.size(48.dp)
                )

                Column {
                    Text(
                        text = clientName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = JivaColors.White
                    )
                    Text(
                        text = businessType,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = JivaColors.White.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = {
                    // Clear saved credentials and logout
                    AuthUtils.logout(context) {
                        onLogout()
                    }
                },
                modifier = Modifier
                    .background(
                        JivaColors.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = JivaColors.White
                )
            }
        }
    }
}

@Composable
private fun ModernMenuCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = menuItem.color.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = JivaColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Colorful icon background
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        menuItem.color.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = menuItem.icon,
                    contentDescription = menuItem.title,
                    modifier = Modifier.size(28.dp),
                    tint = menuItem.color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = menuItem.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                color = Color(0xFF1F2937)
            )
        }
    }
}


@Composable
private fun FooterBranding() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(JivaColors.DeepBlue)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Powered by JIVA • Business Management System",
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = JivaColors.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun SystemStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            StatusRow("Server", "Online", true)
            StatusRow("Database", "Connected", true)
            StatusRow("Authentication", "Active", true)
            StatusRow("Last Updated", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), null)
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String, isOnline: Boolean?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOnline != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when (isOnline) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun HomeScreenPhonePreview() {
    MaterialTheme {
        HomeScreen(
            user = User(
                id = 1,
                username = "demo",
                email = "demo@jiva.com",
                firstName = "Demo",
                lastName = "User",
                role = UserRole.USER
            ),
            onNavigateToScreen = { /* Preview - no navigation */ }
        )
    }
}


@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun HomeScreenTabletPreview() {
    MaterialTheme {
        HomeScreen(
            user = User(
                id = 1,
                username = "demo",
                email = "demo@jiva.com",
                firstName = "Demo",
                lastName = "User",
                role = UserRole.USER
            ),
            onNavigateToScreen = { /* Preview - no navigation */ }
        )
    }
}

