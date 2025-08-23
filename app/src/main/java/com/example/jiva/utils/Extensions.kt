package com.example.jiva.utils

import android.content.Context
import com.example.jiva.JivaApplication

/**
 * Extension functions to make dependency access easier throughout the app
 */

/**
 * Get SyncManager from any Context (Activity, Fragment, etc.)
 * Usage: context.getSyncManager()
 */
fun Context.getSyncManager(): SyncManager {
    return (applicationContext as JivaApplication).syncManager
}

/**
 * Get Repository from any Context
 * Usage: context.getRepository()
 */
fun Context.getRepository() = (applicationContext as JivaApplication).repository

/**
 * Get Database from any Context
 * Usage: context.getDatabase()
 */
fun Context.getDatabase() = (applicationContext as JivaApplication).database

/**
 * Get DataSyncService from any Context
 * Usage: context.getDataSyncService()
 */
fun Context.getDataSyncService() = (applicationContext as JivaApplication).dataSyncService