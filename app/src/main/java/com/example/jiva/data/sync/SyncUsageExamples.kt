package com.example.jiva.data.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.utils.SyncManager
import com.example.jiva.utils.syncAllBusinessData
import com.example.jiva.utils.syncCustomerData
import com.example.jiva.utils.syncInventoryData
import com.example.jiva.utils.syncTransactionData
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Examples of how to use the sync functionality in different scenarios
 * Copy these patterns to your ViewModels or other components
 */

/**
 * Example 1: Using SyncManager in a ViewModel for app startup
 */
class AppStartupViewModel(
    private val syncManager: SyncManager
) : ViewModel() {
    
    /**
     * Call this when the app starts or when user logs in
     * This will sync all business data from server
     */
    fun performInitialDataSync() {
        viewModelScope.launch {
            syncManager.syncAllBusinessData { result ->
                if (result.success) {
                    Timber.i("Initial data sync completed successfully")
                    // Update UI to show sync success
                    // Navigate to main screen or hide loading indicator
                } else {
                    Timber.e("Initial data sync failed: ${result.message}")
                    // Show error message to user
                    // Optionally retry or continue with cached data
                }
            }
        }
    }
}

/**
 * Example 2: Using SyncManager in a screen for manual refresh
 */
class OutstandingReportViewModel(
    private val syncManager: SyncManager
) : ViewModel() {
    
    /**
     * Call this when user pulls to refresh in Outstanding Report screen
     * Only syncs customer-related data for better performance
     */
    fun refreshCustomerData() {
        viewModelScope.launch {
            syncManager.syncCustomerData { success ->
                if (success) {
                    Timber.i("Customer data refreshed successfully")
                    // Update UI with fresh data
                } else {
                    Timber.w("Failed to refresh customer data")
                    // Show error message but continue with cached data
                }
            }
        }
    }
}

/**
 * Example 3: Using DataSyncService directly for more control
 */
class AdminViewModel(
    private val dataSyncService: DataSyncService
) : ViewModel() {
    
    /**
     * Call this from admin panel to sync specific entities
     */
    fun syncSpecificData(entities: Set<EntityType>) {
        viewModelScope.launch {
            val result = dataSyncService.syncSpecificEntities(entities)
            if (result.isSuccess) {
                val results = result.getOrNull()
                results?.forEach { (entity, success) ->
                    if (success) {
                        Timber.d("Successfully synced $entity")
                    } else {
                        Timber.w("Failed to sync $entity")
                    }
                }
            } else {
                Timber.e("Selective sync failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Check sync status before performing operations
     */
    fun checkSyncStatusBeforeOperation() {
        viewModelScope.launch {
            val status = dataSyncService.getSyncStatus()
            when (status.recommendedAction) {
                SyncAction.FULL_SYNC -> {
                    Timber.i("Performing full sync as recommended")
                    performFullSync()
                }
                SyncAction.PARTIAL_SYNC -> {
                    Timber.i("Performing partial sync as recommended")
                    // Sync only critical entities
                    syncSpecificData(setOf(EntityType.ACCOUNTS, EntityType.CLOSING_BALANCES))
                }
                SyncAction.NO_ACTION -> {
                    Timber.d("Data is fresh, no sync needed")
                }
                SyncAction.RETRY_LATER -> {
                    Timber.w("Cannot sync now, will retry later")
                }
            }
        }
    }
    
    private fun performFullSync() {
        viewModelScope.launch {
            val result = dataSyncService.syncAllData()
            result.getOrNull()?.let { syncResult ->
                if (syncResult.success) {
                    Timber.i("Full sync completed in ${syncResult.durationMs}ms")
                } else {
                    Timber.e("Full sync failed: ${syncResult.message}")
                }
            }
        }
    }
}

/**
 * Example 4: Background sync service (can be used with WorkManager)
 */
class BackgroundSyncWorker(
    private val dataSyncService: DataSyncService
) {
    
    /**
     * Perform background sync periodically
     * This can be scheduled with WorkManager
     */
    suspend fun performBackgroundSync(): Boolean {
        return try {
            val result = dataSyncService.syncAllData()
            val syncResult = result.getOrNull()
            
            if (syncResult?.success == true) {
                Timber.i("Background sync completed successfully")
                true
            } else {
                Timber.w("Background sync failed: ${syncResult?.message}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Background sync error")
            false
        }
    }
}

/**
 * Common usage patterns and best practices:
 */
object SyncBestPractices {
    
    /**
     * Pattern 1: App Startup Sync
     * - Use syncAllBusinessData() for initial app load
     * - Show loading screen while syncing
     * - Handle failures gracefully (continue with cached data)
     */
    fun appStartupSync(syncManager: SyncManager, onComplete: (Boolean) -> Unit) {
        syncManager.syncAllBusinessData { result ->
            onComplete(result.success)
        }
    }
    
    /**
     * Pattern 2: Pull-to-Refresh Sync
     * - Use selective sync methods for better performance
     * - Show pull-to-refresh indicator
     * - Update specific UI components on success
     */
    fun pullToRefreshSync(syncManager: SyncManager, screenType: String, onComplete: (Boolean) -> Unit) {
        when (screenType) {
            "outstanding" -> syncManager.syncCustomerData(onComplete)
            "stock" -> syncManager.syncInventoryData(onComplete)
            "sales" -> syncManager.syncTransactionData(onComplete)
            else -> syncManager.syncAllBusinessData { result -> onComplete(result.success) }
        }
    }
    
    /**
     * Pattern 3: Manual Sync Button
     * - Provide manual sync option in settings
     * - Show sync progress and results to user
     * - Allow cancellation of ongoing sync
     */
    fun manualSync(syncManager: SyncManager, onProgress: (String) -> Unit, onComplete: (SyncResult) -> Unit) {
        onProgress("Starting data sync...")
        syncManager.syncAllBusinessData { result ->
            if (result.success) {
                onProgress("Sync completed successfully")
            } else {
                onProgress("Sync failed: ${result.message}")
            }
            onComplete(result)
        }
    }
}

/**
 * Helper extension functions for common sync operations in UI components:
 */

/**
 * Quick sync for Compose UI
 */
fun SyncManager.quickSync(onResult: (Boolean) -> Unit) {
    this.performFullSync { result ->
        onResult(result.success)
    }
}

/**
 * Sync with loading state management
 */
fun SyncManager.syncWithLoading(
    onLoadingChanged: (Boolean) -> Unit,
    onResult: (Boolean) -> Unit
) {
    onLoadingChanged(true)
    this.performFullSync { result ->
        onLoadingChanged(false)
        onResult(result.success)
    }
}