package com.example.jiva.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import com.example.jiva.data.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * DataStore for managing user session persistence
 * This ensures session data survives app restarts and provides better UX
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val LAST_NAME = stringPreferencesKey("last_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val IS_ACTIVE = booleanPreferencesKey("is_active")
        val CREATED_AT = longPreferencesKey("created_at")
        val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_EXPIRES_AT = longPreferencesKey("session_expires_at")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    /**
     * Save user session to DataStore
     */
    suspend fun saveUserSession(session: UserSession) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_ID] = session.user.id
                preferences[PreferencesKeys.USERNAME] = session.user.username
                preferences[PreferencesKeys.EMAIL] = session.user.email ?: ""
                preferences[PreferencesKeys.FIRST_NAME] = session.user.firstName ?: ""
                preferences[PreferencesKeys.LAST_NAME] = session.user.lastName ?: ""
                preferences[PreferencesKeys.USER_ROLE] = session.user.role.name
                preferences[PreferencesKeys.IS_ACTIVE] = session.user.isActive
                preferences[PreferencesKeys.CREATED_AT] = session.user.createdAt
                preferences[PreferencesKeys.LAST_LOGIN_AT] = session.user.lastLoginAt ?: 0L
                preferences[PreferencesKeys.SESSION_TOKEN] = session.token
                preferences[PreferencesKeys.SESSION_EXPIRES_AT] = session.expiresAt
                preferences[PreferencesKeys.IS_LOGGED_IN] = true
            }
            Timber.d("User session saved to DataStore")
        } catch (e: Exception) {
            Timber.e(e, "Error saving user session to DataStore")
        }
    }

    /**
     * Get user session from DataStore
     */
    fun getUserSession(): Flow<UserSession?> {
        return context.dataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading user session from DataStore")
                emit(emptyPreferences())
            }
            .map { preferences ->
                val isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
                
                if (!isLoggedIn) {
                    null
                } else {
                    try {
                        val user = User(
                            id = preferences[PreferencesKeys.USER_ID] ?: 0L,
                            username = preferences[PreferencesKeys.USERNAME] ?: "",
                            email = preferences[PreferencesKeys.EMAIL]?.takeIf { it.isNotBlank() },
                            firstName = preferences[PreferencesKeys.FIRST_NAME]?.takeIf { it.isNotBlank() },
                            lastName = preferences[PreferencesKeys.LAST_NAME]?.takeIf { it.isNotBlank() },
                            role = UserRole.valueOf(preferences[PreferencesKeys.USER_ROLE] ?: UserRole.USER.name),
                            isActive = preferences[PreferencesKeys.IS_ACTIVE] ?: true,
                            createdAt = preferences[PreferencesKeys.CREATED_AT] ?: 0L,
                            lastLoginAt = preferences[PreferencesKeys.LAST_LOGIN_AT]?.takeIf { it > 0L }
                        )
                        
                        UserSession(
                            user = user,
                            token = preferences[PreferencesKeys.SESSION_TOKEN] ?: "",
                            expiresAt = preferences[PreferencesKeys.SESSION_EXPIRES_AT] ?: 0L,
                            isValid = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing user session from DataStore")
                        null
                    }
                }
            }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading login status from DataStore")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
            }
    }

    /**
     * Clear user session from DataStore
     */
    suspend fun clearUserSession() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("User session cleared from DataStore")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing user session from DataStore")
        }
    }

    /**
     * Update session expiry time
     */
    suspend fun updateSessionExpiry(expiresAt: Long) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SESSION_EXPIRES_AT] = expiresAt
            }
            Timber.d("Session expiry updated in DataStore")
        } catch (e: Exception) {
            Timber.e(e, "Error updating session expiry in DataStore")
        }
    }
}
