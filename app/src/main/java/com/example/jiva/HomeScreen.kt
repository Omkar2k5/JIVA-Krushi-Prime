package com.example.jiva

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.data.repository.DummyAuthRepository
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    user: User? = null,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = viewModel { HomeViewModel(DummyAuthRepository()) }
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity)
    val uiState by viewModel.uiState.collectAsState()

    // Get user from session if not provided
    val currentUser = user ?: uiState.currentUser

    // Show success toast when screen is first displayed
    LaunchedEffect(Unit) {
        android.widget.Toast.makeText(context, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.loadUserSession()
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

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Section
        WelcomeSection(currentUser)

        Spacer(modifier = Modifier.height(24.dp))

        // User Info Card
        UserInfoCard(currentUser)

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        QuickActionsSection()

        Spacer(modifier = Modifier.height(24.dp))

        // System Status
        SystemStatusCard()
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
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Welcome to JIVA!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = "You have successfully logged in and your session is active.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun UserInfoCard(currentUser: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "User Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (currentUser != null) {
                UserInfoRow("Username", currentUser.username)
                UserInfoRow("Email", currentUser.email ?: "Not provided")
                UserInfoRow("Full Name", "${currentUser.firstName ?: ""} ${currentUser.lastName ?: ""}".trim().ifEmpty { "Not provided" })
                UserInfoRow("Role", currentUser.role.name.lowercase().replaceFirstChar { it.uppercase() })
                UserInfoRow("Status", if (currentUser.isActive) "Active" else "Inactive")

                if (currentUser.lastLoginAt != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    UserInfoRow("Last Login", dateFormat.format(Date(currentUser.lastLoginAt)))
                }
            } else {
                Text(
                    text = "Loading user information...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun QuickActionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    text = "Profile",
                    onClick = { /* TODO: Navigate to profile */ },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Settings",
                    onClick = { /* TODO: Navigate to settings */ },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Help",
                    onClick = { /* TODO: Navigate to help */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            user = User(
                id = 1,
                username = "demo",
                email = "demo@jiva.com",
                firstName = "Demo",
                lastName = "User",
                role = UserRole.USER
            )
        )
    }
}
