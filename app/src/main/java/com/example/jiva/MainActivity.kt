package com.example.jiva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jiva.data.repository.DummyAuthRepository
import com.example.jiva.data.model.LoginRequest
import com.example.jiva.screens.*
import com.example.jiva.ui.theme.MyApplicationTheme
import com.example.jiva.utils.FileCredentialManager
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JivaApp()
                }
            }
        }
    }
}

@Composable
fun JivaApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Create database instance
    val database = (context.applicationContext as JivaApplication).database
    
    // Create repository instance
    val jivaRepository = remember { 
        com.example.jiva.data.repository.JivaRepositoryImpl(database) 
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash screen to handle auto-login check
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Clear the login screen from the back stack
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        // Clear the home screen from the back stack
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToScreen = { screenId ->
                    navController.navigate(screenId)
                },
                jivaRepository = jivaRepository
            )
        }

        // Menu item screens
        composable("outstanding_report") {
            OutstandingReportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("ledger") {
            LedgerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("stock_report") {
            StockReportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("item_sell_purchase") {
            ItemSellPurchaseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("day_end_report") {
            DayEndReportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("whatsapp_marketing") {
            WhatsAppMarketingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("price_screen") {
            PriceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("expiry_report") {
            ExpiryReportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Add a small delay to show the splash screen
                kotlinx.coroutines.delay(1000)

                val fileCredentialManager = FileCredentialManager.getInstance(context)
                val shouldAutoLogin = fileCredentialManager.shouldAutoLogin()

                if (shouldAutoLogin) {
                    Timber.d("Auto-login enabled, attempting auto-login")

                    // Get saved credentials from file
                    val credentials = fileCredentialManager.loadCredentials()
                    if (credentials != null) {
                        // Perform auto-login
                        val authRepository = DummyAuthRepository()
                        val loginRequest = LoginRequest(
                            username = credentials.username,
                            password = credentials.password
                        )

                        val result = authRepository.login(loginRequest)
                        result.fold(
                            onSuccess = { response ->
                                if (response.success && response.user != null) {
                                    Timber.d("Auto-login successful, navigating to home")
                                    onNavigateToHome()
                                } else {
                                    Timber.w("Auto-login failed, clearing credentials file")
                                    fileCredentialManager.clearCredentials()
                                    onNavigateToLogin()
                                }
                            },
                            onFailure = { exception ->
                                Timber.w("Auto-login failed with exception: ${exception.message}")
                                fileCredentialManager.clearCredentials()
                                onNavigateToLogin()
                            }
                        )
                    } else {
                        Timber.w("No credentials found in file for auto-login")
                        onNavigateToLogin()
                    }
                } else {
                    Timber.d("Auto-login disabled, navigating to login")
                    onNavigateToLogin()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during auto-login check")
                onNavigateToLogin()
            }
        }
    }

    // Show loading screen while checking auto-login
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
