package com.example.jiva

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.ui.theme.Accessibility


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel: LoginViewModel = viewModel ?: run {
        val api = com.example.jiva.data.network.RetrofitClient.jivaApiService
        val repo = com.example.jiva.data.repository.ApiAuthRepository(api)
        viewModel { LoginViewModel(repo, context) }
    }
    val uiState by actualViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Calculate window size for responsive design
    val windowSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity)

    // Handle login success
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
            actualViewModel.resetLoginSuccess()
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
            actualViewModel.resetRateLimit()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Responsive layout based on screen size
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                // Phone layout
                CompactLoginLayout(
                    uiState = uiState,
                    onUsernameChange = actualViewModel::updateUsername,
                    onPasswordChange = actualViewModel::updatePassword,
                    onLogin = actualViewModel::login,
                    onRememberMeChange = actualViewModel::updateRememberMe,
                    onAutoLoginChange = actualViewModel::updateAutoLogin,
                    focusManager = focusManager
                )
            }
            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> {
                // Tablet/Desktop layout
                ExpandedLoginLayout(
                    uiState = uiState,
                    onUsernameChange = actualViewModel::updateUsername,
                    onPasswordChange = actualViewModel::updatePassword,
                    onLogin = actualViewModel::login,
                    onRememberMeChange = actualViewModel::updateRememberMe,
                    onAutoLoginChange = actualViewModel::updateAutoLogin,
                    focusManager = focusManager
                )
            }
        }

        // Snackbar for error messages
        if (showSnackbar && uiState.errorMessage != null) {
            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(3000)
                showSnackbar = false
                actualViewModel.clearError()
            }

            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = {
                        showSnackbar = false
                        actualViewModel.clearError()
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
    onRememberMeChange: (Boolean) -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginContent(
            uiState = uiState,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onLogin = onLogin,
            onRememberMeChange = onRememberMeChange,
            onAutoLoginChange = onAutoLoginChange,
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
    onRememberMeChange: (Boolean) -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginContent(
                    uiState = uiState,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onLogin = onLogin,
                    onRememberMeChange = onRememberMeChange,
                    onAutoLoginChange = onAutoLoginChange,
                    focusManager = focusManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "JIVA Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )

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
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading && !uiState.isRateLimited,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = uiState.errorMessage != null && uiState.username.isBlank()
        )

        // Password Field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "👁️" else "🔒",
                        fontSize = 18.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading && !uiState.isRateLimited,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            isError = uiState.errorMessage != null && uiState.password.isBlank()
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

        // Remember Me and Auto-Login Checkboxes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.rememberMe,
                    onCheckedChange = onRememberMeChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = JivaColors.Purple
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remember username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.autoLogin,
                    onCheckedChange = onAutoLoginChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = JivaColors.Purple
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-login (Keep me signed in)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Login Button
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isLoading &&
                     !uiState.isAutoLoggingIn &&
                     !uiState.isRateLimited &&
                     uiState.username.isNotBlank() &&
                     uiState.password.isNotBlank()
        ) {
            if (uiState.isLoading || uiState.isAutoLoggingIn) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isAutoLoggingIn) "Auto-signing in..." else "Signing in..."
                    )
                }
            } else {
                Text(
                    text = if (uiState.isRateLimited) "Please wait..." else "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
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
