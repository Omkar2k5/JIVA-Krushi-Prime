package com.example.jiva

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jiva.ui.theme.Accessibility
import com.example.jiva.utils.DeviceCompatibility

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Calculate window size for responsive design
    val windowSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity)

    // Handle login success
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
            viewModel.resetLoginSuccess()
        }
    }

    // Show error message as snackbar for better UX
    var showSnackbar by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            showSnackbar = true
        }
    }

    // Auto-reset rate limiting after timeout
    LaunchedEffect(uiState.isRateLimited) {
        if (uiState.isRateLimited) {
            kotlinx.coroutines.delay(30000) // 30 seconds
            viewModel.resetRateLimit()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Responsive layout based on screen size
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                // Phone layout
                CompactLoginLayout(
                    uiState = uiState,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onLogin = viewModel::login,
                    focusManager = focusManager
                )
            }
            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> {
                // Tablet/Desktop layout
                ExpandedLoginLayout(
                    uiState = uiState,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onLogin = viewModel::login,
                    focusManager = focusManager
                )
            }
        }

        // Snackbar for error messages
        if (showSnackbar && uiState.errorMessage != null) {
            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(3000)
                showSnackbar = false
                viewModel.clearError()
            }

            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = {
                        showSnackbar = false
                        viewModel.clearError()
                    }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.errorMessage!!)
            }
        }
    }
}

@Composable
private fun CompactLoginLayout(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val screenPadding = DeviceCompatibility.getScreenPadding()
    val isLandscape = DeviceCompatibility.isLandscape()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isLandscape) Arrangement.Top else Arrangement.Center
    ) {
        if (isLandscape) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        LoginContent(
            uiState = uiState,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onLogin = onLogin,
            focusManager = focusManager,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExpandedLoginLayout(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val screenPadding = DeviceCompatibility.getScreenPadding()
    val contentMaxWidth = DeviceCompatibility.getContentMaxWidth()

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .padding(screenPadding),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginContent(
                    uiState = uiState,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onLogin = onLogin,
                    focusManager = focusManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "JIVA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Welcome back",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Username Field
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .contentDescription(Accessibility.ContentDescriptions.USERNAME_FIELD),
            enabled = !uiState.isLoading && !uiState.isRateLimited,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = uiState.errorMessage != null && uiState.username.isBlank(),
            supportingText = if (uiState.errorMessage != null && uiState.username.isBlank()) {
                { Text("Username is required") }
            } else null
        )

        // Password Field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.contentDescription(
                        if (passwordVisible) Accessibility.ContentDescriptions.HIDE_PASSWORD
                        else Accessibility.ContentDescriptions.SHOW_PASSWORD
                    )
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null // Content description is on the button
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .contentDescription(Accessibility.ContentDescriptions.PASSWORD_FIELD),
            enabled = !uiState.isLoading && !uiState.isRateLimited,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLogin()
                }
            ),
            isError = uiState.errorMessage != null && uiState.password.isBlank(),
            supportingText = if (uiState.errorMessage != null && uiState.password.isBlank()) {
                { Text("Password is required") }
            } else null
        )

        // Rate limiting warning
        if (uiState.isRateLimited) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Too many failed attempts. Please wait 30 seconds before trying again.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Login Button
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .contentDescription(Accessibility.ContentDescriptions.LOGIN_BUTTON),
            enabled = !uiState.isLoading &&
                     !uiState.isRateLimited &&
                     uiState.username.isNotBlank() &&
                     uiState.password.isNotBlank()
        ) {
            if (uiState.isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing in...")
                }
            } else {
                Text(
                    text = if (uiState.isRateLimited) "Please wait..." else "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Demo credentials hint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Demo Accounts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "demo / testing\nadmin / admin123\ntest / test123",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Attempt counter for debugging
        if (uiState.attemptCount > 0) {
            Text(
                text = "Attempts: ${uiState.attemptCount}/5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LoginScreenPhonePreview() {
    MaterialTheme {
        LoginScreen(onLoginSuccess = {})
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun LoginScreenTabletPreview() {
    MaterialTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
