package com.example.jiva.utils

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.data.sync.SyncResult
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Practical example of integrating sync functionality into existing ViewModels
 * Copy these patterns to your actual screens
 */

/**
 * Example: How to add sync to existing ViewModels without @Inject
 * This approach works immediately without changing DI setup
 */
class OutstandingReportViewModelExample(
    private val syncManager: SyncManager
) : ViewModel() {
    
    // UI state
    var isLoading by mutableStateOf(false)
        private set
    
    var syncMessage by mutableStateOf("")
        private set
    
    var lastSyncTime by mutableStateOf(0L)
        private set
    
    /**
     * Call this when screen loads to ensure fresh data
     */
    fun loadInitialData() {
        viewModelScope.launch {
            isLoading = true
            syncMessage = "Loading customer data..."
            
            // Sync only customer-related data for better performance
            syncManager.syncCustomerData { success ->
                isLoading = false
                if (success) {
                    syncMessage = "Data updated successfully"
                    lastSyncTime = System.currentTimeMillis()
                    Timber.i("Outstanding report data synced successfully")
                } else {
                    syncMessage = "Using cached data (sync failed)"
                    Timber.w("Outstanding report sync failed, using cached data")
                }
            }
        }
    }
    
    /**
     * Call this when user pulls to refresh
     */
    fun refreshData() {
        if (!isLoading) {
            loadInitialData()
        }
    }
}

/**
 * ViewModelFactory to create ViewModels without dependency injection
 */
class SyncViewModelFactory(
    private val syncManager: SyncManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            OutstandingReportViewModelExample::class.java -> {
                OutstandingReportViewModelExample(syncManager) as T
            }
            StockViewModelExample::class.java -> {
                StockViewModelExample(syncManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

/**
 * Example: Stock Report ViewModel with inventory sync
 */
class StockViewModelExample(
    private val syncManager: SyncManager
) : ViewModel() {
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    fun loadStockData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            // Sync inventory-specific data
            syncManager.syncInventoryData { success ->
                isLoading = false
                if (!success) {
                    errorMessage = "Failed to update stock data. Showing cached data."
                }
            }
        }
    }
}

/**
 * Compose UI integration examples
 */

/**
 * Example: How to use sync in Compose screens
 */
@Composable
fun OutstandingReportScreenExample() {
    // Get SyncManager from Application
    val context = androidx.compose.ui.platform.LocalContext.current
    val syncManager = remember { context.getSyncManager() }
    
    // Create ViewModel with SyncManager
    val viewModel: OutstandingReportViewModelExample = viewModel(
        factory = SyncViewModelFactory(syncManager)
    )
    
    // Load data when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }
    
    // UI content would go here
    if (viewModel.isLoading) {
        // Show loading indicator
        // LoadingIndicator(message = viewModel.syncMessage)
    } else {
        // Show actual content
        // OutstandingReportContent(onRefresh = { viewModel.refreshData() })
    }
}

/**
 * Example: Background sync on app startup
 */
@Composable
fun AppStartupSyncExample() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val syncManager = remember { context.getSyncManager() }
    
    var syncStatus by remember { mutableStateOf("Checking data...") }
    var isInitialSyncComplete by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        syncManager.syncAllBusinessData { result ->
            syncStatus = if (result.success) {
                "Data synchronized successfully"
            } else {
                "Using cached data (sync failed)"
            }
            isInitialSyncComplete = true
        }
    }
    
    if (!isInitialSyncComplete) {
        // Show splash screen with sync status
        // SplashScreen(message = syncStatus)
    } else {
        // Show main app content
        // MainAppContent()
    }
}

/**
 * Helper functions for common sync patterns
 */
object SyncHelpers {
    
    /**
     * Quick sync for any screen - call from LaunchedEffect
     */
    fun quickScreenSync(
        syncManager: SyncManager,
        screenType: String,
        onComplete: (Boolean) -> Unit
    ) {
        when (screenType.lowercase()) {
            "outstanding", "customer" -> {
                syncManager.syncCustomerData(onComplete)
            }
            "stock", "inventory" -> {
                syncManager.syncInventoryData(onComplete)
            }
            "sales", "purchase", "transactions" -> {
                syncManager.syncTransactionData(onComplete)
            }
            else -> {
                syncManager.syncAllBusinessData { result ->
                    onComplete(result.success)
                }
            }
        }
    }
    
    /**
     * Sync with user feedback - shows success/error messages
     */
    fun syncWithFeedback(
        syncManager: SyncManager,
        onMessage: (String) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        onMessage("Updating data...")
        
        syncManager.syncAllBusinessData { result ->
            val message = if (result.success) {
                "Data updated successfully"
            } else {
                "Update failed: ${result.message}"
            }
            onMessage(message)
            onComplete(result.success)
        }
    }
    
    /**
     * Check if sync is needed before performing expensive operations
     */
    fun conditionalSync(
        syncManager: SyncManager,
        forceSync: Boolean = false,
        onComplete: (Boolean) -> Unit
    ) {
        if (forceSync) {
            syncManager.syncAllBusinessData { result ->
                onComplete(result.success)
            }
        } else {
            syncManager.checkSyncStatus { status ->
                if (status.recommendedAction == com.example.jiva.data.sync.SyncAction.FULL_SYNC ||
                    status.recommendedAction == com.example.jiva.data.sync.SyncAction.PARTIAL_SYNC) {
                    syncManager.syncAllBusinessData { result ->
                        onComplete(result.success)
                    }
                } else {
                    onComplete(true) // Data is fresh
                }
            }
        }
    }
}

/**
 * Usage examples for different scenarios:
 * 
 * 1. Screen loads - sync specific data:
 *    syncManager.syncCustomerData { success -> handleResult(success) }
 * 
 * 2. Pull to refresh - sync relevant data:
 *    SyncHelpers.quickScreenSync(syncManager, "outstanding") { success -> ... }
 * 
 * 3. App startup - sync all data:
 *    syncManager.syncAllBusinessData { result -> navigateToMain(result.success) }
 * 
 * 4. Manual refresh button:
 *    SyncHelpers.syncWithFeedback(syncManager, ::showMessage) { success -> ... }
 * 
 * 5. Before critical operation:
 *    SyncHelpers.conditionalSync(syncManager, forceSync = true) { success -> ... }
 */