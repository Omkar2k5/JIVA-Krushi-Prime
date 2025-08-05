package com.example.jiva

import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.LoginResponse
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import com.example.jiva.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for LoginViewModel
 * Essential for production deployment with 100+ users
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateUsername should sanitize and update username`() = runTest {
        // Given
        val username = "  testuser  "
        
        // When
        viewModel.updateUsername(username)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("testuser", uiState.username)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `updatePassword should update password and clear errors`() = runTest {
        // Given
        val password = "testpassword"
        
        // When
        viewModel.updatePassword(password)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(password, uiState.password)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `login with empty username should show error`() = runTest {
        // Given
        viewModel.updateUsername("")
        viewModel.updatePassword("password")
        
        // When
        viewModel.login()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("Username is required", uiState.errorMessage)
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isLoginSuccessful)
    }

    @Test
    fun `login with empty password should show error`() = runTest {
        // Given
        viewModel.updateUsername("testuser")
        viewModel.updatePassword("")
        
        // When
        viewModel.login()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("Password is required", uiState.errorMessage)
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isLoginSuccessful)
    }

    @Test
    fun `login with invalid username format should show error`() = runTest {
        // Given
        viewModel.updateUsername("ab") // Too short
        viewModel.updatePassword("password123")
        
        // When
        viewModel.login()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("Invalid username format", uiState.errorMessage)
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isLoginSuccessful)
    }

    @Test
    fun `successful login should update state correctly`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        val user = User(
            id = 1,
            username = username,
            email = "test@example.com",
            role = UserRole.USER
        )
        val loginResponse = LoginResponse(
            success = true,
            user = user,
            token = "test-token",
            message = "Login successful"
        )
        
        whenever(authRepository.login(LoginRequest(username, password)))
            .thenReturn(Result.success(loginResponse))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        
        // When
        viewModel.login()
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isLoginSuccessful)
        assertFalse(uiState.isLoading)
        assertNull(uiState.errorMessage)
        assertEquals(user, uiState.user)
        assertEquals(0, uiState.attemptCount)
    }

    @Test
    fun `failed login should update attempt count`() = runTest {
        // Given
        val username = "testuser"
        val password = "wrongpassword"
        val loginResponse = LoginResponse(
            success = false,
            message = "Invalid credentials"
        )
        
        whenever(authRepository.login(LoginRequest(username, password)))
            .thenReturn(Result.success(loginResponse))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        
        // When
        viewModel.login()
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoginSuccessful)
        assertFalse(uiState.isLoading)
        assertEquals("Invalid credentials", uiState.errorMessage)
        assertEquals(1, uiState.attemptCount)
    }

    @Test
    fun `clearError should remove error message`() = runTest {
        // Given
        viewModel.updateUsername("")
        viewModel.login() // This will set an error
        
        // When
        viewModel.clearError()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `resetLoginSuccess should reset login success state`() = runTest {
        // Given - simulate successful login
        val username = "testuser"
        val password = "password123"
        val user = User(id = 1, username = username, role = UserRole.USER)
        val loginResponse = LoginResponse(success = true, user = user, token = "token")
        
        whenever(authRepository.login(LoginRequest(username, password)))
            .thenReturn(Result.success(loginResponse))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        viewModel.login()
        advanceUntilIdle()
        
        // When
        viewModel.resetLoginSuccess()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoginSuccessful)
    }
}
