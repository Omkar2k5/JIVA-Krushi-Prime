package com.example.jiva.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
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
import com.example.jiva.data.repository.JivaRepositoryImpl
import com.example.jiva.viewmodel.WhatsAppViewModel

// Data model for Customer contact information
data class CustomerContact(
    val accountNumber: String,
    val accountName: String,
    val mobileNumber: String,
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppBulkMessageScreenImpl(onBackClick: () -> Unit = {}) {
    // State management
    var messageText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectAll by remember { mutableStateOf(false) }
    
    // Get the context and database
    val context = LocalContext.current
    val database = (context.applicationContext as JivaApplication).database
    
    // Create repository and view model
    val jivaRepository = remember { JivaRepositoryImpl(database) }
    val viewModel: WhatsAppViewModel = viewModel { WhatsAppViewModel(jivaRepository) }

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
        // Header with gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(JivaColors.DeepBlue, JivaColors.Purple)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(
                                JivaColors.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JivaColors.White
                        )
                    }
                    
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "JIVA Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Column {
                        Text(
                            text = "WhatsApp Bulk Message",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White
                        )
                        Text(
                            text = "Send messages to multiple customers",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = JivaColors.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // WhatsApp icon
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "WhatsApp",
                    tint = JivaColors.Green,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            JivaColors.White,
                            CircleShape
                        )
                        .padding(8.dp)
                )
            }
        }

        // Main content with performance optimizations
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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

                        // Image URL input
                        Text(
                            text = "Image URL (optional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = JivaColors.DeepBlue
                        )
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            placeholder = { Text("https://example.com/image.jpg") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = JivaColors.Purple
                            )
                        )

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

            // Customer Table Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Customer Contacts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JivaColors.DeepBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Table Header
                        CustomerTableHeader()
                    }
                }
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                item {
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
            } else if (customerContacts.isEmpty()) {
                item {
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
            }

            // Customer Table Data Items with keys for performance
            items(
                items = customerContacts.withIndex().toList(),
                key = { (_, contact) -> contact.accountNumber }
            ) { (index, contact) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
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
                            
                            // Show toast with selected customers count
                            android.widget.Toast.makeText(
                                context,
                                "Sending message to ${selectedCustomers.size} customers",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            
                            // In real implementation, this would send messages via WhatsApp Business API
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
