package com.example.jiva

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.data.repository.DummyAuthRepository
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a menu item in the home screen
 */
data class MenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String = ""
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    user: User? = null,
    onLogout: () -> Unit = {},
    onNavigateToScreen: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel { HomeViewModel(DummyAuthRepository()) }
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity)
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()

    // Get user from session if not provided
    val currentUser = user ?: uiState.currentUser

    // Show success toast when screen is first displayed
    LaunchedEffect(Unit) {
        android.widget.Toast.makeText(context, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.loadUserSession()
    }

    // Define menu items
    val menuItems = remember {
        listOf(
            MenuItem(
                id = "outstanding_report",
                title = "Outstanding Report",
                icon = Icons.Default.Assessment,
                description = "View outstanding payments and dues"
            ),
            MenuItem(
                id = "ledger",
                title = "Ledger",
                icon = Icons.Default.AccountBalance,
                description = "Account ledger and transactions"
            ),
            MenuItem(
                id = "stock_report",
                title = "Stock Report",
                icon = Icons.Default.Inventory,
                description = "Current stock levels and inventory"
            ),
            MenuItem(
                id = "item_sell_purchase",
                title = "Item Sell/Purchase",
                icon = Icons.Default.ShoppingCart,
                description = "Manage sales and purchases"
            ),
            MenuItem(
                id = "day_end_report",
                title = "Day End Report",
                icon = Icons.Default.Today,
                description = "Daily summary and closing report"
            ),
            MenuItem(
                id = "whatsapp_marketing",
                title = "WhatsApp Bulk Marketing",
                icon = Icons.Default.Message,
                description = "Send bulk marketing messages"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top App Bar
        TopAppBar(
            currentUser = currentUser,
            onLogout = {
                viewModel.logout()
                onLogout()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Welcome Section
        WelcomeSection(currentUser)

        Spacer(modifier = Modifier.height(24.dp))

        // Main Menu Grid
        MenuGrid(
            menuItems = menuItems,
            windowSizeClass = windowSizeClass,
            onItemClick = onNavigateToScreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        // System Status
        SystemStatusCard()
    }
}

@Composable
private fun MenuGrid(
    menuItems: List<MenuItem>,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onItemClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Determine grid columns based on screen size and orientation
    val columns = when {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> {
            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        }
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Main Menu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height((menuItems.size / columns + if (menuItems.size % columns > 0) 1 else 0) * 120.dp)
            ) {
                items(menuItems) { item ->
                    MenuCard(
                        menuItem = item,
                        onClick = { onItemClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = menuItem.icon,
                contentDescription = menuItem.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    currentUser: User?,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "JIVA Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentUser != null) {
                        Text(
                            text = "Welcome, ${currentUser.firstName ?: currentUser.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WelcomeSection(currentUser: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👋",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column {
                Text(
                    text = "Welcome back${if (currentUser?.firstName != null) ", ${currentUser.firstName}" else ""}!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Choose an option from the menu below",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
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
