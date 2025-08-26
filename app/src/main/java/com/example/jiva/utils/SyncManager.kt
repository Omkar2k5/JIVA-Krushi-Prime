package com.example.jiva.utils

import com.example.jiva.data.sync.DataSyncService
import com.example.jiva.data.sync.SyncResult
import com.example.jiva.data.sync.EntityType
import com.example.jiva.data.sync.SyncAction
import com.example.jiva.data.sync.SyncStatus
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * High-level sync manager for the JIVA application
 * Provides easy-to-use methods for triggering data synchronization
 * Can be used by UI components, background services, and app initialization
 */
class SyncManager(
    private val dataSyncService: DataSyncService
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Perform a complete data sync from server to local database
     * This should be called:
     * - On app startup
     * - When user manually requests refresh
     * - Periodically in background
     * 
     * @param onComplete Callback with sync result
     */
    fun performFullSync(
        onComplete: ((SyncResult) -> Unit)? = null
    ) {
        syncScope.launch {
            try {
                Timber.i("Initiating full data sync...")
                val result = dataSyncService.syncAllData()
                
                val syncResult = result.getOrNull() ?: SyncResult(
                    success = false,
                    message = "Sync service returned null result",
                    error = result.exceptionOrNull()
                )
                
                // Switch to Main dispatcher for callback
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(syncResult)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Exception during full sync")
                val syncResult = SyncResult(
                    success = false,
                    message = "Sync manager error: ${e.message}",
                    error = e
                )
                
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(syncResult)
                }
            }
        }
    }

    /**
     * Perform sync for specific data types only
     * Useful when you know only certain data needs updating
     * 
     * @param entities Set of entity types to sync
     * @param onComplete Callback with results for each entity type
     */
    fun performSelectiveSync(
        entities: Set<EntityType>,
        onComplete: ((Map<EntityType, Boolean>) -> Unit)? = null
    ) {
        syncScope.launch {
            try {
                Timber.i("Initiating selective sync for: ${entities.joinToString()}")
                val result = dataSyncService.syncSpecificEntities(entities)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        onComplete?.invoke(result.getOrNull() ?: emptyMap())
                    } else {
                        onComplete?.invoke(entities.associateWith { false })
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Exception during selective sync")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(entities.associateWith { false })
                }
            }
        }
    }

    /**
     * Check if sync is needed and get recommendations
     * Call this to determine what kind of sync operation to perform
     */
    fun checkSyncStatus(
        onResult: (SyncStatus) -> Unit
    ) {
        syncScope.launch {
            try {
                val status = dataSyncService.getSyncStatus()
                withContext(Dispatchers.Main) {
                    onResult(status)
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception getting sync status")
                withContext(Dispatchers.Main) {
                    onResult(SyncStatus(
                        lastSyncTimestamp = 0,
                        isDataFresh = false,
                        canSync = false,
                        recommendedAction = SyncAction.RETRY_LATER,
                        error = "Error checking sync status: ${e.message}"
                    ))
                }
            }
        }
    }

    /**
     * Cancel all ongoing sync operations
     */
    fun cancelAllSyncs() {
        syncScope.coroutineContext.cancelChildren()
        Timber.i("All sync operations cancelled")
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        syncScope.cancel()
        Timber.d("SyncManager cleaned up")
    }
}

/**
 * Simple sync status for UI components
 */


/**
 * Extension functions for easy sync operations in UI components
 */

/**
 * Sync all business data
 * Usage: syncManager.syncAllBusinessData { result -> handleResult(result) }
 */
fun SyncManager.syncAllBusinessData(onComplete: (SyncResult) -> Unit) {
    this.performFullSync(onComplete)
}

/**
 * Sync only customer and account data
 * Usage: syncManager.syncCustomerData { success -> handleSuccess(success) }
 */
fun SyncManager.syncCustomerData(onComplete: (Boolean) -> Unit) {
    val customerEntities = setOf(
        EntityType.ACCOUNTS,
        EntityType.CLOSING_BALANCES
    )
    this.performSelectiveSync(customerEntities) { results ->
        val success = results.values.all { it }
        onComplete(success)
    }
}

/**
 * Sync only inventory data
 * Usage: syncManager.syncInventoryData { success -> handleSuccess(success) }
 */
fun SyncManager.syncInventoryData(onComplete: (Boolean) -> Unit) {
    val inventoryEntities = setOf(
        EntityType.STOCKS,
        EntityType.EXPIRIES,
        EntityType.PRICE_DATA
    )
    this.performSelectiveSync(inventoryEntities) { results ->
        val success = results.values.all { it }
        onComplete(success)
    }
}

/**
 * Sync only transaction data
 * Usage: syncManager.syncTransactionData { success -> handleSuccess(success) }
 */
fun SyncManager.syncTransactionData(onComplete: (Boolean) -> Unit) {
    val transactionEntities = setOf(
        EntityType.SALE_PURCHASES,
        EntityType.LEDGERS
    )
    this.performSelectiveSync(transactionEntities) { results ->
        val success = results.values.all { it }
        onComplete(success)
    }
}