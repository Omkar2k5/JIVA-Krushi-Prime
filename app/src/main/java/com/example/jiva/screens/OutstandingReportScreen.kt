package com.example.jiva.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jiva.JivaColors
import com.example.jiva.components.ResponsiveReportHeader
import com.example.jiva.utils.UserEnv
import com.example.jiva.viewmodel.OutstandingReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data model for Outstanding Report entries (kept as Strings for simplicity)
data class OutstandingEntry(
    val acId: String,
    val accountName: String,
    val mobile: String,
    val under: String,
    val balance: String,
    val lastDate: String,
    val days: String,
    val creditLimitAmount: String,
    val creditLimitDays: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreenImpl(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel using database factory
    val db = com.example.jiva.data.database.JivaDatabase.getDatabase(context.applicationContext)
    val viewModel: OutstandingReportViewModel = viewModel(
        factory = OutstandingReportViewModel.Factory(db)
    )

    val uiState by viewModel.uiState.collectAsState()

    // UI State
    var outstandingOf by remember { mutableStateOf("Customer") }
    var partyNameSearch by remember { mutableStateOf("") }
    var mobileNumberSearch by remember { mutableStateOf("") }
    var hasClickedShow by remember { mutableStateOf(false) }

    var selectedEntries by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }

    // WhatsApp template config from UserEnv
    val (whatsappTemplate, waInstanceId, waAccessToken) = remember {
        try {
            val json = UserEnv.getMsgTemplatesJson(context)
            val companyName = UserEnv.getCompanyName(context) ?: ""
            if (!json.isNullOrBlank()) {
                val gson = com.google.gson.Gson()
                val items = gson.fromJson(
                    json,
                    Array<com.example.jiva.data.api.models.MsgTemplateItem>::class.java
                )?.toList() ?: emptyList()
                val outTemplate = items.firstOrNull { it.category.equals("OutStandingReport", true) }
                val rawMsg = outTemplate?.msg ?: ""
                val preview = rawMsg
                    .replace("{CmpName}", companyName)
                    .replace("{add1}", "Shop Address 1")
                    .replace("{add2}", "Shop Address 2")
                    .replace("{add3}", "Shop Address 3")
                Triple(preview, outTemplate?.instanceID.orEmpty(), outTemplate?.accessToken.orEmpty())
            } else Triple("", "", "")
        } catch (e: Exception) {
            Timber.w(e, "Failed to load WhatsApp template from env")
            Triple("", "", "")
        }
    }

    // Session
    val year = UserEnv.getFinancialYear(context) ?: "2025-26"
    val finalUserId = UserEnv.getUserId(context)?.toIntOrNull()

    if (finalUserId == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(JivaColors.LightGray),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "User session not initialized. Please login again.",
                color = JivaColors.Red
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "If you are logged in, please go back and reopen the screen.",
                color = JivaColors.DarkGray
            )
        }
        return
    }

    // Auto-load for default selection Customer -> under = Sundry debtors
    LaunchedEffect(finalUserId, year) {
        val uid = finalUserId ?: return@LaunchedEffect
        try {
            viewModel.fetchOutstandingFiltered(
                userId = uid,
                year = year,
                accountName = null,
                area = null,
                under = "Sundry debtors"
            )
        } catch (e: Exception) {
            Timber.e(e, "initial outstanding fetch crashed")
        }
    }

    // Debounce text filters
    var partyNameQuery by remember { mutableStateOf("") }
    var mobileQuery by remember { mutableStateOf("") }

    LaunchedEffect(partyNameSearch) {
        delay(250)
        partyNameQuery = partyNameSearch
    }
    LaunchedEffect(mobileNumberSearch) {
        delay(250)
        mobileQuery = mobileNumberSearch
    }

    // Filter list
    val filteredEntriesAfterSearch = remember(mobileQuery, uiState.outstandingEntries) {
        if (mobileQuery.isBlank()) uiState.outstandingEntries else uiState.outstandingEntries.filter {
            it.mobile.contains(mobileQuery, ignoreCase = true)
        }
    }
    val finalEntries = remember(partyNameQuery, mobileQuery, filteredEntriesAfterSearch) {
        try {
            if (filteredEntriesAfterSearch.isEmpty()) emptyList() else filteredEntriesAfterSearch.filter { entry ->
                try {
                    val nameMatch = if (partyNameQuery.isBlank()) true else entry.accountName.contains(partyNameQuery, true)
                    val mobileMatch = if (mobileQuery.isBlank()) true else entry.mobile.contains(mobileQuery, true)
                    nameMatch && mobileMatch
                } catch (e: Exception) {
                    Timber.e(e, "Error filtering entry: ${'$'}{entry.acId}")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during filtering")
            emptyList()
        }
    }

    // Select All handling
    LaunchedEffect(selectAll, finalEntries) {
        selectedEntries = if (selectAll) finalEntries.asSequence().map { it.acId }.toSet() else emptySet()
    }
    LaunchedEffect(selectedEntries, finalEntries) {
        val displayedIds = finalEntries.asSequence().map { it.acId }.toSet()
        selectAll = displayedIds.isNotEmpty() && selectedEntries.containsAll(displayedIds)
    }

    // Total balance
    val totalBalance = remember(finalEntries) {
        try {
            finalEntries.sumOf { entry ->
                try {
                    val clean = entry.balance
                        .replace("₹", "")
                        .replace(",", "")
                        .replace(" ", "")
                        .trim()
                    clean.toDoubleOrNull() ?: 0.0
                } catch (e: Exception) { 0.0 }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating total balance")
            0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JivaColors.LightGray)
    ) {
        ResponsiveReportHeader(
            title = "Outstanding Report",
            subtitle = "Manage outstanding payments and dues",
            onBackClick = onBackClick,
            actions = { }
        )

        // Error screen
        if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to load Outstanding data.\n${'$'}{uiState.error}",
                    color = JivaColors.Red,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        // Global loading
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = JivaColors.DeepBlue,
                    strokeWidth = 4.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = true
            ) {
                // Control Panel Card
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
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Customer/Supplier selector
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Outstanding Of",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = outstandingOf == "Customer",
                                                onClick = { outstandingOf = "Customer" }
                                            )
                                            Text("Customer", fontSize = 14.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = outstandingOf == "Supplier",
                                                onClick = { outstandingOf = "Supplier" }
                                            )
                                            Text("Supplier", fontSize = 14.sp)
                                        }
                                    }
                                }

                                // Show button
                                Button(
                                    onClick = {
                                        val uid = finalUserId ?: return@Button
                                        hasClickedShow = true
                                        selectedEntries = emptySet()
                                        val requestedUnder = if (outstandingOf.equals("Customer", true))
                                            "Sundry debtors" else "Sundry creditors"
                                        viewModel.fetchOutstandingFiltered(
                                            userId = uid,
                                            year = year,
                                            accountName = null,
                                            area = null,
                                            under = requestedUnder
                                        )
                                    },
                                    enabled = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JivaColors.DeepBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Show",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Show", color = Color.White)
                                }

                                // Inline loader while fetching
                                if (hasClickedShow && uiState.isLoading) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = JivaColors.DeepBlue,
                                        trackColor = JivaColors.LightGray
                                    )
                                }

                                // Filters after data loads
                                if (!uiState.isLoading && uiState.outstandingEntries.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = partyNameSearch,
                                        onValueChange = { partyNameSearch = it },
                                        placeholder = { Text("Search by name or ID...") },
                                        trailingIcon = {
                                            IconButton(onClick = { partyNameSearch = "" }) {
                                                Icon(
                                                    imageVector = if (partyNameSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Visibility,
                                                    contentDescription = if (partyNameSearch.isNotEmpty()) "Clear" else "Search"
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            cursorColor = JivaColors.DeepBlue
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = mobileNumberSearch,
                                        onValueChange = { mobileNumberSearch = it },
                                        placeholder = { Text("Search by mobile...") },
                                        trailingIcon = {
                                            IconButton(onClick = { mobileNumberSearch = "" }) {
                                                Icon(
                                                    imageVector = if (mobileNumberSearch.isNotEmpty()) Icons.Default.Clear else Icons.Default.Phone,
                                                    contentDescription = if (mobileNumberSearch.isNotEmpty()) "Clear" else "Mobile"
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            cursorColor = JivaColors.DeepBlue
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // WhatsApp Messaging Section
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
                                text = "WhatsApp Message",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected: ${'$'}{selectedEntries.size} of ${'$'}{uiState.outstandingEntries.size} entries",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JivaColors.DeepBlue
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectAll,
                                        onCheckedChange = { selectAll = it },
                                        colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                                    )
                                    Text(
                                        text = "Select All",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DeepBlue
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Message Template:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JivaColors.DarkGray
                                    )
                                    Text(
                                        text = whatsappTemplate,
                                        fontSize = 14.sp,
                                        color = JivaColors.DeepBlue,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) {
                                        scope.launch {
                                            sendWhatsAppMessages(
                                                context = context,
                                                entries = finalEntries,
                                                selectedIds = selectedEntries,
                                                templatePreview = whatsappTemplate,
                                                instanceId = waInstanceId,
                                                accessToken = waAccessToken
                                            )
                                        }
                                    }
                                },
                                enabled = selectedEntries.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    disabledContainerColor = JivaColors.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send WhatsApp to ${'$'}{selectedEntries.size} contacts",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Share PDF Button
                item {
                    val tableWidth = 1302.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.width(tableWidth),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        generateAndSharePDF(context, finalEntries, totalBalance)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = JivaColors.Purple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = JivaColors.White
                                    )
                                    Text(
                                        text = "SHARE REPORT",
                                        color = JivaColors.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Data Table Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JivaColors.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Outstanding Report Data",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JivaColors.DeepBlue,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val tableScrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.horizontalScroll(tableScrollState)
                                ) {
                                    OutstandingTableHeader()

                                    if (uiState.isLoading) {
                                        repeat(5) { OutstandingLoadingRow() }
                                    } else if (finalEntries.isNotEmpty()) {
                                        val lazyState = rememberLazyListState()
                                        LazyColumn(
                                            state = lazyState,
                                            modifier = Modifier.heightIn(min = 240.dp, max = 600.dp)
                                        ) {
                                            itemsIndexed(
                                                items = finalEntries as List<OutstandingEntry>,
                                                key = { index: Int, item: OutstandingEntry -> item.acId.ifBlank { index.toString() } }
                                            ) { _: Int, entry: OutstandingEntry ->
                                                OutstandingTableRow(
                                                    entry = entry,
                                                    isSelected = selectedEntries.contains(entry.acId),
                                                    onSelectionChange = { isSelected: Boolean ->
                                                        selectedEntries = if (isSelected) selectedEntries + entry.acId else selectedEntries - entry.acId
                                                    }
                                                )
                                            }
                                            item { OutstandingTotalRow(totalBalance = totalBalance) }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = JivaColors.LightGray)
                                        ) {
                                            Text(
                                                text = "No outstanding data found. Click Show to load data.",
                                                modifier = Modifier.padding(16.dp),
                                                fontSize = 14.sp,
                                                color = JivaColors.DarkGray,
                                                textAlign = TextAlign.Center
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
    }
}

@Composable
private fun OutstandingTableHeader() {
    Row(
        modifier = Modifier
            .background(JivaColors.LightGray, RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Select", tint = JivaColors.DeepBlue, modifier = Modifier.size(16.dp))
        }
        OutstandingHeaderCell("AC ID", Modifier.width(80.dp))
        OutstandingHeaderCell("Account Name", Modifier.width(180.dp))
        OutstandingHeaderCell("Mobile", Modifier.width(140.dp))
        OutstandingHeaderCell("Under", Modifier.width(160.dp))
        OutstandingHeaderCell("Balance", Modifier.width(120.dp))
        OutstandingHeaderCell("Last Date", Modifier.width(140.dp))
        OutstandingHeaderCell("Days", Modifier.width(80.dp))
        OutstandingHeaderCell("Credit Limit Amt", Modifier.width(140.dp))
        OutstandingHeaderCell("Credit Limit Days", Modifier.width(140.dp))
    }
}

@Composable
private fun OutstandingHeaderCell(text: String, modifier: Modifier = Modifier) {
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
private fun OutstandingTableRow(
    entry: OutstandingEntry,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange,
            colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
        )
        OutstandingCell(entry.acId, Modifier.width(80.dp))
        OutstandingCell(entry.accountName, Modifier.width(180.dp))
        OutstandingCell(entry.mobile, Modifier.width(140.dp))
        OutstandingCell(entry.under, Modifier.width(160.dp))
        OutstandingCell(entry.balance, Modifier.width(120.dp), color = if ((entry.balance.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0) >= 0) JivaColors.Green else JivaColors.Red)
        OutstandingCell(entry.lastDate, Modifier.width(140.dp))
        OutstandingCell(entry.days, Modifier.width(80.dp))
        OutstandingCell(entry.creditLimitAmount, Modifier.width(140.dp))
        OutstandingCell(entry.creditLimitDays, Modifier.width(140.dp))
    }
    Divider(color = JivaColors.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun OutstandingCell(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151)
) {
    val safeText = remember(text) {
        try { text.takeIf { it.isNotBlank() } ?: "" } catch (e: Exception) { Timber.e(e, "Error processing text: ${'$'}text"); "Error" }
    }
    Text(
        text = safeText,
        fontSize = 11.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun OutstandingLoadingRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .shimmerEffect(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(20.dp)
                .background(JivaColors.LightGray, RoundedCornerShape(4.dp))
        )
        repeat(8) { index ->
            val width = when (index) {
                0 -> 80.dp
                1 -> 180.dp
                2 -> 140.dp
                3 -> 160.dp
                4 -> 120.dp
                5 -> 140.dp
                6 -> 80.dp
                7 -> 140.dp
                else -> 140.dp
            }
            Box(
                modifier = Modifier
                    .width(width)
                    .height(16.dp)
                    .background(JivaColors.LightGray, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun OutstandingTotalRow(totalBalance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JivaColors.DeepBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(50.dp))
        Text(
            text = "TOTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JivaColors.DeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(560.dp)
        )
        OutstandingCell(
            text = "₹" + String.format("%.2f", totalBalance),
            modifier = Modifier.width(120.dp),
            color = if (totalBalance >= 0) JivaColors.Green else JivaColors.Red
        )
        repeat(3) { Box(modifier = Modifier.width(140.dp)) }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1000)),
        label = "shimmer"
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFB8B5B5), Color(0xFF8F8B8B), Color(0xFFB8B5B5)),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).also { mod ->
        // Use layout to capture size without onGloballyPositioned if extension import missing
        mod.then(Modifier)
    }
}

private suspend fun sendWhatsAppMessages(
    context: Context,
    entries: List<OutstandingEntry>,
    selectedIds: Set<String>,
    templatePreview: String,
    instanceId: String,
    accessToken: String
) {
    try {
        val selectedEntries = entries.filter { it.acId in selectedIds }
        val companyName = UserEnv.getCompanyName(context) ?: ""
        val staticAdd1 = "Shop Address 1"
        val staticAdd2 = "Shop Address 2"
        val staticAdd3 = "Shop Address 3"

        for (entry in selectedEntries) {
            if (entry.mobile.isNotBlank()) {
                val msg = templatePreview
                    .replace("{CmpName}", companyName)
                    .replace("{add1}", staticAdd1)
                    .replace("{add2}", staticAdd2)
                    .replace("{add3}", staticAdd3)
                    .replace("[customer]", entry.accountName)
                    .replace("[TM]", entry.balance)
                    .replace("[Mobile]", entry.mobile)

                val digitsOnly = entry.mobile.filter { it.isDigit() }
                val normalized = if (digitsOnly.startsWith("91")) digitsOnly else "91${'$'}digitsOnly"
                val chatId = "${'$'}normalized@c.us"

                if (instanceId.isBlank() || accessToken.isBlank()) {
                    Timber.w("InstanceId or AccessToken missing, skipping send for ${'$'}{entry.mobile}")
                } else {
                    try {
                        val client = okhttp3.OkHttpClient()
                        val url = "https://api.green-api.com/waInstance${'$'}instanceId/sendMessage/${'$'}accessToken"
                        val jsonObj = mapOf("chatId" to chatId, "message" to msg)
                        val jsonStr = com.google.gson.Gson().toJson(jsonObj)
                        val mediaType = "application/json; charset=utf-8".toMediaType()
                        val body = jsonStr.toRequestBody(mediaType)
                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .build()
                        withContext(Dispatchers.IO) {
                            client.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    Timber.e("Green API send failed for ${'$'}{entry.mobile}: ${'$'}{response.code} ${'$'}{response.message}")
                                } else {
                                    Timber.d("Green API send success for ${'$'}{entry.mobile}")
                                }
                            }
                        }
                        delay(500)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send via Green API to ${'$'}{entry.mobile}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error in bulk WhatsApp messaging")
    }
}

private suspend fun generateAndSharePDF(
    context: Context,
    entries: List<OutstandingEntry>,
    totalBalance: Double
) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }

            val startX = 30f
            val startY = 120f
            val rowHeight = 25f
            val colWidths = floatArrayOf(60f, 140f, 100f, 90f, 100f)
            val totalWidth = colWidths.sum()
            val headers = arrayOf("AC ID", "Account Name", "Mobile", "Balance", "Area")

            val maxRowsPerPage = 25
            val totalPages = kotlin.math.ceil(entries.size.toDouble() / maxRowsPerPage).toInt().coerceAtLeast(1)
            var entryIndex = 0

            for (pageNum in 1..totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Outstanding Report", 297.5f, 50f, titlePaint)
                canvas.drawText("Generated on: ${'$'}currentDate", 297.5f, 75f, cellPaint)
                canvas.drawText("Page ${'$'}pageNum of ${'$'}totalPages", 297.5f, 95f, cellPaint)

                var currentX = startX
                var currentY = startY

                val headerRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                canvas.drawRect(headerRect, android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; style = android.graphics.Paint.Style.FILL })

                for (i in headers.indices) {
                    val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(headers[i], currentX + 5f, currentY + 15f, headerPaint)
                    currentX += colWidths[i]
                }

                currentY += rowHeight
                val endIndex = kotlin.math.min(entryIndex + maxRowsPerPage, entries.size)
                for (i in entryIndex until endIndex) {
                    val entry = entries[i]
                    currentX = startX
                    val rowData = arrayOf(
                        entry.acId,
                        entry.accountName.take(20),
                        entry.mobile,
                        "₹${'$'}{entry.balance}",
                        entry.under.take(12)
                    )
                    for (j in rowData.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[j], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        canvas.drawText(rowData[j], currentX + 5f, currentY + 15f, cellPaint)
                        currentX += colWidths[j]
                    }
                    currentY += rowHeight
                }

                entryIndex = endIndex

                if (pageNum == totalPages) {
                    currentX = startX
                    val totalRect = RectF(startX, currentY, startX + totalWidth, currentY + rowHeight)
                    canvas.drawRect(totalRect, android.graphics.Paint().apply { color = android.graphics.Color.CYAN; style = android.graphics.Paint.Style.FILL; alpha = 100 })
                    val totalData = arrayOf("TOTAL", "", "", "₹" + String.format("%.0f", totalBalance), "")
                    for (i in totalData.indices) {
                        val rect = RectF(currentX, currentY, currentX + colWidths[i], currentY + rowHeight)
                        canvas.drawRect(rect, borderPaint)
                        canvas.drawText(totalData[i], currentX + 5f, currentY + 15f, headerPaint)
                        currentX += colWidths[i]
                    }
                    canvas.drawText("Total Entries: ${'$'}{entries.size}", startX, currentY + 50f, cellPaint)
                    canvas.drawText("Generated by JIVA App", startX, currentY + 70f, cellPaint)
                }

                pdfDocument.finishPage(page)
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Outstanding_Report_" + timestamp + ".pdf"
            val file = File(downloadsDir, fileName)

            val out = FileOutputStream(file)
            pdfDocument.writeTo(out)
            out.close()
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                sharePDF(context, file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun sharePDF(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${'$'}{context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Outstanding Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the Outstanding Report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Outstanding Report")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
    }
}

@Preview(showBackground = true)
@Composable
private fun OutstandingReportScreenPreview() {
    OutstandingReportScreenImpl()
}