package com.example.jiva.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaApplication
import com.example.jiva.JivaColors
import com.example.jiva.R
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.data.repository.JivaRepositoryImpl
import com.example.jiva.viewmodel.WhatsAppViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// Data model for Customer contact information
data class CustomerContact(
    val accountNumber: String,
    val accountName: String,
    val mobileNumber: String,
    val messageStatus: String = "Pending",
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WhatsAppBulkMessageScreenImpl(onBackClick: () -> Unit = {}) {
    // State management
    var messageText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectAll by remember { mutableStateOf(false) }
    
    // File selection and upload states
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Get the context and database
    val context = LocalContext.current
    val database = (context.applicationContext as JivaApplication).database
    val scope = rememberCoroutineScope()
    
    // Create repository and view model
    val jivaRepository = remember { JivaRepositoryImpl(database) }
    val viewModel: WhatsAppViewModel = viewModel { WhatsAppViewModel(jivaRepository) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uploadError = null
        uploadedImageUrl = null
    }
    
    // Upload function
    fun uploadImage(uri: Uri) {
        scope.launch {
            try {
                isUploading = true
                uploadProgress = 0f
                uploadError = null
                
                // Simulate upload progress
                for (i in 1..10) {
                    uploadProgress = i * 0.1f
                    kotlinx.coroutines.delay(200)
                }
                
                // Call actual API through repository
                val result = jivaRepository.uploadImage(uri)
                if (result.isSuccess) {
                    uploadedImageUrl = result.getOrNull()
                    imageUrl = uploadedImageUrl ?: ""
                    
                    android.widget.Toast.makeText(
                        context,
                        "Image uploaded successfully!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Upload failed"
                    uploadError = error
                    android.widget.Toast.makeText(
                        context,
                        "Upload failed: $error",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                uploadError = e.message ?: "Upload failed"
                android.widget.Toast.makeText(
                    context,
                    "Upload failed: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } finally {
                isUploading = false
            }
        }
    }

    // On open: fetch Sundry debtors via Outstanding API
    LaunchedEffect(Unit) {
        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
        val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"
        if (userId != null) {
            viewModel.loadCustomerContactsFromOutstanding(userId, year)
        } else {
            // Fallback to DB if session missing
            viewModel.loadCustomerContacts()
        }
    }
    
    // Observe UI state
    val uiState by viewModel.uiState.collectAsState()
    val customerContacts = uiState.customerContacts
    
    // Calculate selected count
    val selectedCount = customerContacts.count { it.isSelected }
    val totalCount = customerContacts.size
    
    // Show toast for error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            android.widget.Toast.makeText(
                context,
                error,
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearError()
        }
    }
    
    // Handle select all functionality
    LaunchedEffect(selectAll) {
        viewModel.selectAllContacts(selectAll)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        // Report-style header (like Price List)
        ResponsiveReportHeader(
            title = "WhatsApp Bulk Messaging",
            subtitle = "Send messages to multiple customers",
            onBackClick = onBackClick,
            actions = { }
        )

        // Main content with performance optimizations and sticky header
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
            // Message Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Compose Message",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue
                        )

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text("Enter your WhatsApp message here...")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = JivaColors.Purple
                            )
                        )

                        // Image selection and upload section
                        Text(
                            text = "Attach Image",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = JivaColors.DeepBlue
                        )
                        
                        // Image preview and file selection
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { filePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedImageUri != null) JivaColors.LightBlue.copy(alpha = 0.1f) else JivaColors.LightGray.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (selectedImageUri != null) JivaColors.Purple else JivaColors.DarkGray.copy(alpha = 0.3f)
                            )
                        ) {
                            if (selectedImageUri != null) {
                                // Show selected image preview
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedImageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Show placeholder
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Select Image",
                                        tint = JivaColors.DarkGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap to select an image",
                                        fontSize = 12.sp,
                                        color = JivaColors.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        // Upload button and progress
                        if (selectedImageUri != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { uploadImage(selectedImageUri!!) },
                                    enabled = !isUploading,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JivaColors.Purple
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isUploading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = JivaColors.White,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Uploading...",
                                                color = JivaColors.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Upload",
                                                tint = JivaColors.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Upload Image",
                                                color = JivaColors.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = { 
                                        selectedImageUri = null
                                        uploadedImageUrl = null
                                        imageUrl = ""
                                        uploadError = null
                                    },
                                    enabled = !isUploading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JivaColors.Red.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = JivaColors.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Upload progress bar
                            if (isUploading) {
                                LinearProgressIndicator(
                                    progress = uploadProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = JivaColors.Purple,
                                    trackColor = JivaColors.LightGray
                                )
                            }
                            
                            // Upload error message
                            uploadError?.let { error ->
                                Text(
                                    text = "Upload failed: $error",
                                    fontSize = 12.sp,
                                    color = JivaColors.Red,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // Success message
                            if (uploadedImageUrl != null) {
                                Text(
                                    text = "✓ Image uploaded successfully!",
                                    fontSize = 12.sp,
                                    color = JivaColors.Green,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Character count
                        Text(
                            text = "${messageText.length}/1000 characters",
                            fontSize = 12.sp,
                            color = if (messageText.length > 1000) JivaColors.Red else JivaColors.DeepBlue.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Selection Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = JivaColors.Teal
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recipients Selected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.White
                            )
                            Text(
                                text = "$selectedCount of $totalCount customers",
                                fontSize = 14.sp,
                                color = JivaColors.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Select All Button
                            Button(
                                onClick = { 
                                    selectAll = !selectAll
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (selectAll) "Deselect All" else "Select All",
                                    color = JivaColors.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Customer Contacts Table (header + rows in single component)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Contacts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )
                            Text(
                                text = "${customerContacts.size} customers",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JivaColors.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Table container with horizontal scroll and fixed height (like Price screen)
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header
                            CustomerTableHeader()

                            // Content
                            when {
                                uiState.isLoading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = JivaColors.DeepBlue,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Loading customer contacts...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = JivaColors.DeepBlue
                                            )
                                        }
                                    }
                                }

                                customerContacts.isEmpty() -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No customer contacts found with mobile numbers",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                else -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(
                                            items = customerContacts,
                                            key = { it.accountNumber }
                                        ) { contact ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(0.dp),
                                                colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                CustomerTableRow(
                                                    contact = contact,
                                                    onSelectionChanged = { isSelected ->
                                                        viewModel.updateContactSelection(contact.accountNumber, isSelected)
                                                        // Update selectAll state based on current selections
                                                        selectAll = customerContacts.all { it.isSelected }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Send Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = JivaColors.Green
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Button(
                        onClick = { 
                            // Get selected customers from the ViewModel
                            val selectedCustomers = customerContacts.filter { it.isSelected }
                            
                            // Prepare message data
                            val messageData = mapOf(
                                "message" to messageText,
                                "imageUrl" to (uploadedImageUrl ?: ""),
                                "recipients" to selectedCustomers.map { it.mobileNumber }
                            )
                            
                            // Show toast with selected customers count and image status
                            val imageStatus = if (uploadedImageUrl != null) " with image" else " without image"
                            android.widget.Toast.makeText(
                                context,
                                "Sending message to ${selectedCustomers.size} customers$imageStatus",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            
                            // TODO: In real implementation, this would send messages via WhatsApp Business API
                            // The messageData contains all necessary information for the API call
                        },
                        enabled = selectedCount > 0 && messageText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send WhatsApp",
                                tint = JivaColors.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (selectedCount > 0) "Send WhatsApp to $selectedCount Recipients" else "Select Recipients to Send",
                                color = JivaColors.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JivaColors.LightGray, RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox column header (match Outstanding style)
        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Select", tint = JivaColors.DeepBlue, modifier = Modifier.size(16.dp))
        }
        CustomerHeaderCell("AC ID", Modifier.width(80.dp))
        CustomerHeaderCell("Account Name", Modifier.width(180.dp))
        CustomerHeaderCell("Mobile", Modifier.width(140.dp))
        CustomerHeaderCell("Message Status", Modifier.width(140.dp))
    }
}

@Composable
private fun CustomerHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun CustomerTableRow(
    contact: CustomerContact,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = contact.isSelected,
            onCheckedChange = onSelectionChanged,
            colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
        )
        CustomerCell(contact.accountNumber, Modifier.width(80.dp))
        CustomerCell(contact.accountName, Modifier.width(180.dp))
        CustomerCell(contact.mobileNumber, Modifier.width(140.dp))
        CustomerCell(contact.messageStatus, Modifier.width(140.dp))
    }
    Divider(color = JivaColors.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun CustomerCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun WhatsAppBulkMessageScreenPreview() {
    WhatsAppBulkMessageScreenImpl()
}
