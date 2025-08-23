package com.example.jiva.data.sync

import com.example.jiva.data.repository.JivaRepository
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for synchronizing data between server and local database
 * Handles both full sync and individual entity sync operations
 */
class DataSyncService(
    private val repository: JivaRepository
) {

    /**
     * Sync all data from server to local database
     * This is the primary sync method that uses the /api/sync-all-data endpoint
     * 
     * @return Result indicating success or failure with error details
     */
    suspend fun syncAllData(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            Timber.i("Starting complete data synchronization...")
            val startTime = System.currentTimeMillis()
            
            // Call the main sync method from repository
            val result = repository.syncAllData()
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            if (result.isSuccess) {
                val syncResult = SyncResult(
                    success = true,
                    message = "Successfully synced all data from server",
                    durationMs = duration,
                    timestamp = System.currentTimeMillis()
                )
                Timber.i("Data sync completed successfully in ${duration}ms")
                Result.success(syncResult)
            } else {
                val error = result.exceptionOrNull()
                val syncResult = SyncResult(
                    success = false,
                    message = "Data sync failed: ${error?.message ?: "Unknown error"}",
                    error = error,
                    durationMs = duration,
                    timestamp = System.currentTimeMillis()
                )
                Timber.e(error, "Data sync failed after ${duration}ms")
                Result.success(syncResult) // Return successful Result with failed SyncResult
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during data synchronization")
            val syncResult = SyncResult(
                success = false,
                message = "Sync service error: ${e.message}",
                error = e,
                durationMs = 0,
                timestamp = System.currentTimeMillis()
            )
            Result.success(syncResult)
        }
    }

    /**
     * Sync specific entity types individually
     * Useful for partial updates or when full sync fails
     */
    suspend fun syncSpecificEntities(
        entities: Set<EntityType>
    ): Result<Map<EntityType, Boolean>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<EntityType, Boolean>()
        
        try {
            Timber.i("Starting selective entity sync for: ${entities.joinToString()}")
            
            entities.forEach { entityType ->
                try {
                    val result = when (entityType) {
                        EntityType.USERS -> repository.syncUsers()
                        EntityType.ACCOUNTS -> repository.syncAccounts()
                        EntityType.CLOSING_BALANCES -> repository.syncClosingBalances()
                        EntityType.STOCKS -> repository.syncStocks()
                        EntityType.SALE_PURCHASES -> repository.syncSalePurchases()
                        EntityType.LEDGERS -> repository.syncLedgers()
                        EntityType.EXPIRIES -> repository.syncExpiries()
                        EntityType.TEMPLATES -> repository.syncTemplates()
                        EntityType.PRICE_DATA -> repository.syncPriceData()
                    }
                    results[entityType] = result.isSuccess
                    if (result.isSuccess) {
                        Timber.d("Successfully synced $entityType")
                    } else {
                        Timber.w("Failed to sync $entityType: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception syncing $entityType")
                    results[entityType] = false
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Timber.e(e, "Exception during selective entity sync")
            Result.failure(e)
        }
    }

    /**
     * Check sync status and provide recommendations
     */
    suspend fun getSyncStatus(): SyncStatus = withContext(Dispatchers.IO) {
        try {
            // You could implement logic here to check:
            // - Last sync timestamp
            // - Data freshness
            // - Network connectivity
            // - Server availability
            
            SyncStatus(
                lastSyncTimestamp = 0, // Get from preferences or database
                isDataFresh = false, // Check if data is within acceptable age
                canSync = true, // Check network and server availability
                recommendedAction = SyncAction.FULL_SYNC
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting sync status")
            SyncStatus(
                lastSyncTimestamp = 0,
                isDataFresh = false,
                canSync = false,
                recommendedAction = SyncAction.RETRY_LATER,
                error = e.message
            )
        }
    }
}

/**
 * Data classes for sync operations
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val error: Throwable? = null,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncStatus(
    val lastSyncTimestamp: Long,
    val isDataFresh: Boolean,
    val canSync: Boolean,
    val recommendedAction: SyncAction,
    val error: String? = null
)

enum class EntityType {
    USERS,
    ACCOUNTS,
    CLOSING_BALANCES,
    STOCKS,
    SALE_PURCHASES,
    LEDGERS,
    EXPIRIES,
    TEMPLATES,
    PRICE_DATA
}

enum class SyncAction {
    FULL_SYNC,
    PARTIAL_SYNC,
    NO_ACTION,
    RETRY_LATER
}