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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors
import com.example.jiva.R

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
    var selectAll by remember { mutableStateOf(false) }
    
    // Dummy customer data
    val customerContacts = remember {
        mutableStateListOf(
            CustomerContact("001", "Aman Shaikh", "+91 9876543210"),
            CustomerContact("002", "ABC Traders", "+91 9876543211"),
            CustomerContact("003", "XYZ Suppliers", "+91 9876543212"),
            CustomerContact("004", "PQR Industries", "+91 9876543213"),
            CustomerContact("005", "LMN Corporation", "+91 9876543214"),
            CustomerContact("006", "DEF Enterprises", "+91 9876543215"),
            CustomerContact("007", "GHI Solutions", "+91 9876543216"),
            CustomerContact("008", "Tech Agro", "+91 9876543217"),
            CustomerContact("009", "Modern Farms", "+91 9876543218"),
            CustomerContact("010", "Green Valley", "+91 9876543219"),
            CustomerContact("011", "Sunrise Agro", "+91 9876543220"),
            CustomerContact("012", "Golden Harvest", "+91 9876543221"),
            CustomerContact("013", "Fresh Fields", "+91 9876543222"),
            CustomerContact("014", "Crop Care Ltd", "+91 9876543223"),
            CustomerContact("015", "Agri Solutions", "+91 9876543224")
        )
    }

    // Calculate selected count
    val selectedCount = customerContacts.count { it.isSelected }
    val totalCount = customerContacts.size

    // Handle select all functionality
    LaunchedEffect(selectAll) {
        customerContacts.forEachIndexed { index, contact ->
            customerContacts[index] = contact.copy(isSelected = selectAll)
        }
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
                            imageVector = Icons.Default.ArrowBack,
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
                                .height(150.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 8,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                            customerContacts[index] = contact.copy(isSelected = isSelected)
                            // Update selectAll state based on current selections
                            selectAll = customerContacts.all { it.isSelected }
                        }
                    )
                }
            }

            // Table Footer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            // TODO: Send WhatsApp messages to selected customers
                            val selectedCustomers = customerContacts.filter { it.isSelected }
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
            .background(
                JivaColors.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox column header
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = JivaColors.DeepBlue
            )
        }

        CustomerHeaderCell("Account No.", modifier = Modifier.weight(1f))
        CustomerHeaderCell("Account Name", modifier = Modifier.weight(2f))
        CustomerHeaderCell("Mobile Number", modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun CustomerHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = JivaColors.DeepBlue,
        textAlign = TextAlign.Start,
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = contact.isSelected,
                    onCheckedChange = onSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = JivaColors.Green,
                        uncheckedColor = JivaColors.DeepBlue.copy(alpha = 0.6f)
                    )
                )
            }

            CustomerCell(contact.accountNumber, modifier = Modifier.weight(1f))
            CustomerCell(contact.accountName, modifier = Modifier.weight(2f))
            CustomerCell(contact.mobileNumber, modifier = Modifier.weight(1.5f))
        }

        HorizontalDivider(
            color = JivaColors.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
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
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun WhatsAppBulkMessageScreenPreview() {
    WhatsAppBulkMessageScreenImpl()
}
