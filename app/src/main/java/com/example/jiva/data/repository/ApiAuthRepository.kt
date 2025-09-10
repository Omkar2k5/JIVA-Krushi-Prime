package com.example.jiva.data.repository

import com.example.jiva.data.api.JivaApiService
import com.example.jiva.data.api.models.ApiLoginRequest
import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.LoginResponse
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import com.example.jiva.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Real AuthRepository implementation that calls the HTTP API.
 */
class ApiAuthRepository(
    private val api: JivaApiService
) : AuthRepository {

    private val _currentSession = MutableStateFlow<UserSession?>(null)

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val apiRes = api.login(ApiLoginRequest(mobileNo = request.username, password = request.password))
            
            Timber.d("Login API Response: isSuccess=${apiRes.isSuccess}, userID=${apiRes.data?.userID}, isActive=${apiRes.data?.isActive}, validTill='${apiRes.data?.validTill}', companyName=${apiRes.data?.companyName}")
            
            if (apiRes.isSuccess && apiRes.data?.userID != null) {
                // Validate isActive field
                if (apiRes.data.isActive != "1") {
                    Timber.w("Login failed: User account is not active (isActive = ${apiRes.data.isActive})")
                    return Result.success(
                        LoginResponse(
                            success = false,
                            message = "Account is not active. Please contact support."
                        )
                    )
                }
                
                // Validate validTill field - MUST have a valid future date
                val validTill = apiRes.data.validTill
                if (validTill.isNullOrBlank()) {
                    Timber.w("Login failed: Account has no validity date (validTill is empty/null)")
                    return Result.success(
                        LoginResponse(
                            success = false,
                            message = "Account validity expired. Please contact support."
                        )
                    )
                } else {
                    val isValid = validateAccountValidity(validTill)
                    if (!isValid) {
                        Timber.w("Login failed: Account validity expired (validTill = $validTill)")
                        return Result.success(
                            LoginResponse(
                                success = false,
                                message = "Account validity expired. Please contact support."
                            )
                        )
                    }
                }
                
                // Login successful - create user session
                val user = User(
                    id = apiRes.data.userID.toLongOrNull() ?: 0L,
                    username = request.username,
                    email = null,
                    firstName = apiRes.data.companyName,
                    lastName = null,
                    role = UserRole.USER
                )
                val session = UserSession(
                    user = user,
                    token = apiRes.data.userID ?: "",
                    expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                )
                _currentSession.value = session
                
                Timber.d("Login successful for user ${apiRes.data.userID}, company: ${apiRes.data.companyName}")
                Result.success(
                    LoginResponse(
                        success = true,
                        user = user,
                        token = session.token,
                        message = apiRes.message,
                        expiresAt = session.expiresAt
                    )
                )
            } else {
                Timber.w("Login failed: API returned isSuccess=${apiRes.isSuccess}, userID=${apiRes.data?.userID}")
                Result.success(
                    LoginResponse(
                        success = false,
                        message = apiRes.message ?: "Login failed"
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Login API call failed")
            Result.failure(e)
        }
    }
    
    /**
     * Validate account validity based on validTill date
     * Expected formats: "9/1/2027 12:00:00 AM" or "01/09/2027 12:00:00 AM"
     */
    private fun validateAccountValidity(validTill: String): Boolean {
        if (validTill.isBlank()) {
            Timber.w("validTill is blank, blocking login")
            return false
        }
        
        return try {
            val currentDate = Date()
            var validTillDate: Date? = null
            
            // Try multiple date formats
            val dateFormats = listOf(
                SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US),
                SimpleDateFormat("MM/dd/yyyy h:mm:ss a", Locale.US),
                SimpleDateFormat("d/M/yyyy h:mm:ss a", Locale.US),
                SimpleDateFormat("dd/MM/yyyy h:mm:ss a", Locale.US),
                SimpleDateFormat("M/d/yyyy", Locale.US),
                SimpleDateFormat("MM/dd/yyyy", Locale.US)
            )
            
            for (format in dateFormats) {
                try {
                    validTillDate = format.parse(validTill)
                    if (validTillDate != null) {
                        break
                    }
                } catch (e: Exception) {
                    // Try next format
                    continue
                }
            }
            
            if (validTillDate == null) {
                Timber.w("Could not parse validTill date with any format: $validTill, blocking login")
                return false
            }
            
            val isValid = validTillDate.after(currentDate)
            Timber.d("Account validity check: validTill=$validTill, parsed=${validTillDate}, current=${currentDate}, isValid=$isValid")
            isValid
        } catch (e: Exception) {
            Timber.e(e, "Error parsing validTill date: $validTill")
            // If we can't parse the date, block login for security
            false
        }
    }

    override suspend fun logout(): Result<Unit> {
        _currentSession.value = null
        return Result.success(Unit)
    }

    override suspend fun getCurrentSession(): UserSession? = _currentSession.value

    override fun observeCurrentSession(): Flow<UserSession?> = _currentSession.asStateFlow()

    override suspend fun isSessionValid(): Boolean {
        val s = _currentSession.value
        return s != null && s.expiresAt > System.currentTimeMillis() && s.isValid
    }

    override suspend fun refreshSession(): Result<UserSession> {
        val s = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        val refreshed = s.copy(expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
        _currentSession.value = refreshed
        return Result.success(refreshed)
    }

    override suspend fun getUserByUsername(username: String): User? = _currentSession.value?.user?.takeIf { it.username == username }

    override suspend fun clearSession() { _currentSession.value = null }
}