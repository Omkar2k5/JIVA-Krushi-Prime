package com.example.jiva.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.example.jiva.data.api.models.MsgTemplateItem
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
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
    val area: String,
    val balance: String,
    val lastDate: String,
    val days: String,
    val creditLimitAmount: String,
    val creditLimitDays: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreenImpl(onBackClick: () -> Unit = {}) {
    // Bulk send progress state
    var isBulkSending by remember { mutableStateOf(false) }
    var bulkSent by remember { mutableStateOf(0) }
    var bulkTotal by remember { mutableStateOf(0) }
    var etaLeftSec by remember { mutableStateOf(0) }
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

    // SMS Permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "SMS permission is required to send messages", Toast.LENGTH_LONG).show()
        }
    }

    // WhatsApp/SMS template config from UserEnv (force Outstanding template)
    val (whatsappTemplate, waInstanceId, waAccessToken) = remember {
        try {
            val templateJson = UserEnv.getMsgTemplatesJson(context)
            val templates = Gson().fromJson(templateJson, Array<MsgTemplateItem>::class.java)?.toList().orEmpty()

            fun norm(s: String?) = s?.lowercase()?.replace(Regex("[\\s_-]+"), "")
            val candidates = setOf("outstandingreport", "outstanding", "outstandingbalance", "osr")

            // Strictly prefer Outstanding; do not pick unrelated templates
            val outTemplate = templates.firstOrNull { norm(it.category) in candidates }
                ?: templates.firstOrNull { it.category?.contains("outstanding", ignoreCase = true) == true }

            if (outTemplate != null) {
                val rawMsg = outTemplate.msg ?: "test message"
                val companyName = UserEnv.getCompanyName(context) ?: ""
                val add1 = UserEnv.getAddress1(context) ?: ""
                val add2 = UserEnv.getAddress2(context) ?: ""
                val add3 = UserEnv.getAddress3(context) ?: ""
                val preview = rawMsg
                    .replace("{CmpName}", companyName)
                    .replace("{add1}", add1)
                    .replace("{add2}", add2)
                    .replace("{add3}", add3)
                Triple(preview, outTemplate.instanceID.orEmpty(), outTemplate.accessToken.orEmpty())
            } else {
                // No Outstanding template found; use safe neutral
                Triple("test message", "", "")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load WhatsApp template")
            Triple("test message", "", "")
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectAll,
                                    onCheckedChange = { isChecked -> selectAll = isChecked },
                                    colors = CheckboxDefaults.colors(checkedColor = JivaColors.Purple)
                                )
                                Text("Select all contacts")
                            }

                            // WhatsApp Button
                            Button(
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) {
                                        scope.launch {
                                            // If exactly one selection -> single-user API call
                                            if (selectedEntries.size == 1) {
                                                val acId = selectedEntries.first()
                                                val entry = finalEntries.firstOrNull { it.acId == acId }
                                                if (entry != null) {
                                                    val digits = entry.mobile.filter { it.isDigit() }
                                                    val number = when (digits.length) {
                                                        10 -> "91$digits"
                                                        12 -> digits
                                                        else -> entry.mobile.trim()
                                                    }
                                                    val companyName = UserEnv.getCompanyName(context) ?: ""
                                                    val add1 = UserEnv.getAddress1(context) ?: ""
                                                    val add2 = UserEnv.getAddress2(context) ?: ""
                                                    val add3 = UserEnv.getAddress3(context) ?: ""
                                                    val msg = whatsappTemplate
                                                        .replace("{CmpName}", companyName)
                                                        .replace("{add1}", add1)
                                                        .replace("{add2}", add2)
                                                        .replace("{add3}", add3)
                                                        .replace("[customer]", entry.accountName)
                                                        .replace("[TM]", entry.balance)
                                                        .replace("[Mobile]", entry.mobile)
                                                        .ifBlank { "test message" }
                                                    val (ok, _) = com.example.jiva.data.network.JivabotApi.send(
                                                        number = number,
                                                        type = "text",
                                                        message = msg,
                                                        instanceId = waInstanceId,
                                                        accessToken = waAccessToken
                                                    )
                                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            if (ok) "Message sent" else "Failed to send message",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                // Bulk send with progress updates
                                                isBulkSending = true
                                                bulkTotal = selectedEntries.size
                                                bulkSent = 0
                                                etaLeftSec = bulkTotal * 10
                                                sendWhatsAppMessages(
                                                    context = context,
                                                    entries = finalEntries,
                                                    selectedIds = selectedEntries,
                                                    templatePreview = whatsappTemplate,
                                                    instanceId = waInstanceId,
                                                    accessToken = waAccessToken
                                                ) { sentCount, totalCount ->
                                                    bulkSent = sentCount
                                                    bulkTotal = totalCount
                                                    etaLeftSec = (totalCount - sentCount) * 10
                                                    if (sentCount == totalCount) {
                                                        isBulkSending = false
                                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Message Sent Successfully",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            }
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
                                    text = "Send WhatsApp",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // SMS Button
                            Button(
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) {
                                        // Check SMS permission
                                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)) {
                                            PackageManager.PERMISSION_GRANTED -> {
                                                val selectedCustomers = finalEntries.filter { selectedEntries.contains(it.acId) }
                                                scope.launch {
                                                    sendBulkSMS(
                                                        context = context,
                                                        customers = selectedCustomers,
                                                        template = whatsappTemplate,
                                                        onProgress = { sent, total, eta ->
                                                            bulkSent = sent
                                                            bulkTotal = total
                                                            etaLeftSec = eta
                                                        },
                                                        onStart = { isBulkSending = true },
                                                        onComplete = { 
                                                            isBulkSending = false
                                                            selectedEntries = emptySet()
                                                        }
                                                    )
                                                }
                                            }
                                            else -> {
                                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please select customers first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = selectedEntries.isNotEmpty() && !isBulkSending,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JivaColors.Orange,
                                    disabledContainerColor = JivaColors.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send SMS (${selectedEntries.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isBulkSending) {
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = if (bulkTotal > 0) bulkSent.toFloat() / bulkTotal.toFloat() else 0f,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = JivaColors.DeepBlue,
                                    trackColor = JivaColors.LightGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
                                                items = finalEntries,
                                                key = { index: Int, item: OutstandingEntry -> "${index}_${item.acId}" }
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
        OutstandingHeaderCell("Area", Modifier.width(120.dp))
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
        OutstandingCell(entry.area, Modifier.width(120.dp))
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
            modifier = Modifier.width(80.dp)
        )
        // Empty spaces for Account Name, Mobile, Under, Area columns
        Box(modifier = Modifier.width(180.dp))
        Box(modifier = Modifier.width(140.dp))
        Box(modifier = Modifier.width(160.dp))
        Box(modifier = Modifier.width(120.dp))
        // Balance column
        OutstandingCell(
            text = "₹" + String.format("%.2f", totalBalance),
            modifier = Modifier.width(120.dp),
            color = if (totalBalance >= 0) JivaColors.Green else JivaColors.Red
        )
        // Empty spaces for remaining columns
        Box(modifier = Modifier.width(140.dp))
        Box(modifier = Modifier.width(80.dp))
        Box(modifier = Modifier.width(140.dp))
        Box(modifier = Modifier.width(140.dp))
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
    accessToken: String,
    onProgress: ((sent: Int, total: Int) -> Unit)? = null
) {
    try {
        val selectedEntries = entries.filter { it.acId in selectedIds }
        val companyName = UserEnv.getCompanyName(context) ?: ""
        val staticAdd1 = UserEnv.getAddress1(context) ?: ""
        val staticAdd2 = UserEnv.getAddress2(context) ?: ""
        val staticAdd3 = UserEnv.getAddress3(context) ?: ""

        // Validate credentials once before sending; show toast and return if invalid
        if (instanceId.isBlank() || accessToken.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Invalid WhatsApp credentials. Please set Instance ID and Access Token.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        var invalidCredToastShown = false
        var sent = 0
        val total = selectedEntries.size
        onProgress?.invoke(sent, total)

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

                // Normalize number: add 91 for 10-digit numbers, pass-through for 12-digit
                val digits = entry.mobile.filter { it.isDigit() }
                val numberRaw = when (digits.length) {
                    10 -> "91$digits"
                    12 -> digits
                    else -> entry.mobile.trim()
                }

                if (instanceId.isBlank() || accessToken.isBlank()) {
                    Timber.w("InstanceId or AccessToken missing, skipping send for ${'$'}{entry.mobile}")
                } else {
                    try {
                        // Use centralized Jivabot API helper
                        val (ok, _) = com.example.jiva.data.network.JivabotApi.send(
                            number = numberRaw,
                            type = "text",
                            message = msg,
                            instanceId = instanceId,
                            accessToken = accessToken
                        )
                        if (!ok) {
                            if (!invalidCredToastShown) {
                                invalidCredToastShown = true
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to send via Jivabot. Check connectivity or credentials.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        // Progress update after each send
                        sent += 1
                        onProgress?.invoke(sent, total)
                        // Random delay 8-15 seconds between each message
                        val delayMs = (8..15).random() * 1000L
                        delay(delayMs)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send via Jivabot to ${'$'}{entry.mobile}")
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
            // Landscape A4 page: 842 x 595
            val pageWidth = 842
            val pageHeight = 595
            val margin = 30f
            val contentWidth = pageWidth - (2 * margin)
            val pdfDocument = PdfDocument()

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }
            val cellPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9f
                typeface = Typeface.DEFAULT
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }
            val fillHeaderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = android.graphics.Paint.Style.FILL
            }

            val startX = margin
            val startY = 90f
            val rowHeight = 18f

            // 10 columns to match on-screen table
            val headers = arrayOf(
                "AC ID", "Account Name", "Mobile", "Under", "Area", "Balance",
                "Last Date", "Days", "Credit Limit Amt", "Credit Limit Days"
            )
            // Column widths as percentages of content width (sum = 1.0)
            val colPercents = floatArrayOf(0.07f, 0.18f, 0.11f, 0.13f, 0.10f, 0.11f, 0.10f, 0.05f, 0.08f, 0.07f)
            val colWidths = FloatArray(headers.size) { contentWidth * colPercents[it] }
            val totalWidth = colWidths.sum()

            // Helper to clamp text within a cell width
            fun drawClampedText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, width: Float, paint: android.graphics.Paint) {
                val ellipsis = "…"
                val maxLen = paint.breakText(text, true, width - 6f, null)
                val toDraw = if (maxLen < text.length && maxLen > 1) text.substring(0, maxLen - 1) + ellipsis else text
                canvas.drawText(toDraw, x + 5f, y, paint)
            }

            // Compute rows per page for available height
            val footerReserve = 60f
            val availableHeight = pageHeight - startY - footerReserve
            val maxRowsPerPage = kotlin.math.floor(availableHeight / rowHeight).toInt().coerceAtLeast(1)

            val totalPages = kotlin.math.ceil(entries.size.toDouble() / maxRowsPerPage).toInt().coerceAtLeast(1)
            var entryIndex = 0

            for (pageNum in 1..totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Outstanding Report", pageWidth / 2f, 40f, titlePaint)
                canvas.drawText("Generated on: ${currentDate}", pageWidth / 2f, 60f, cellPaint)
                canvas.drawText("Page ${pageNum} of ${totalPages}", pageWidth / 2f, 75f, cellPaint)

                var currentX = startX
                var currentY = startY

                // Header background and text
                val headerRect = RectF(startX, currentY - rowHeight + 2f, startX + totalWidth, currentY + 2f)
                canvas.drawRect(headerRect, fillHeaderPaint)
                for (i in headers.indices) {
                    val rect = RectF(currentX, currentY - rowHeight, currentX + colWidths[i], currentY)
                    canvas.drawRect(rect, borderPaint)
                    drawClampedText(canvas, headers[i], currentX, currentY - 6f, colWidths[i], headerPaint)
                    currentX += colWidths[i]
                }

                // Rows
                var rowsDrawn = 0
                val endIndex = kotlin.math.min(entryIndex + maxRowsPerPage, entries.size)
                while (entryIndex < endIndex) {
                    val e = entries[entryIndex]
                    currentX = startX
                    currentY += rowHeight

                    val rowData = arrayOf(
                        e.acId,
                        e.accountName,
                        e.mobile,
                        e.under,
                        e.area,
                        "₹${e.balance}",
                        e.lastDate,
                        e.days,
                        e.creditLimitAmount,
                        e.creditLimitDays
                    )
                    for (i in rowData.indices) {
                        val rect = RectF(currentX, currentY - rowHeight, currentX + colWidths[i], currentY)
                        canvas.drawRect(rect, borderPaint)
                        drawClampedText(canvas, rowData[i], currentX, currentY - 6f, colWidths[i], cellPaint)
                        currentX += colWidths[i]
                    }
                    rowsDrawn++
                    entryIndex++
                }

                // Footer totals on last page
                if (pageNum == totalPages) {
                    currentX = startX
                    currentY += rowHeight
                    val totalRect = RectF(startX, currentY - rowHeight, startX + totalWidth, currentY)
                    val totalFill = android.graphics.Paint().apply { color = android.graphics.Color.CYAN; style = android.graphics.Paint.Style.FILL; alpha = 60 }
                    canvas.drawRect(totalRect, totalFill)

                    // Draw cells and put total under Balance column
                    var xCursor = startX
                    for (i in headers.indices) {
                        val rect = RectF(xCursor, currentY - rowHeight, xCursor + colWidths[i], currentY)
                        canvas.drawRect(rect, borderPaint)
                        if (headers[i] == "Balance") {
                            drawClampedText(canvas, "₹" + String.format(Locale.getDefault(), "%.2f", totalBalance), xCursor, currentY - 6f, colWidths[i], headerPaint)
                        } else if (i == 0) {
                            drawClampedText(canvas, "TOTAL", xCursor, currentY - 6f, colWidths[i], headerPaint)
                        }
                        xCursor += colWidths[i]
                    }
                }

                pdfDocument.finishPage(page)
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Outstanding_Report_" + timestamp + ".pdf"
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                sharePDF(context, file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun sharePDF(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Outstanding Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the Outstanding Report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Grant URI permission to potential receivers
        context.packageManager.queryIntentActivities(shareIntent, 0).forEach { ri ->
            val packageName = ri.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Outstanding Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Send SMS to multiple customers with random delay between messages
 */
private suspend fun sendBulkSMS(
    context: Context,
    customers: List<OutstandingEntry>,
    template: String,
    onProgress: (sent: Int, total: Int, eta: Int) -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    withContext(Dispatchers.IO) {
        onStart()
        
        Timber.d("sendBulkSMS called with ${customers.size} customers")
        
        val validCustomers = customers.filter { customer ->
            val hasValidMobile = customer.mobile.isNotBlank() && customer.mobile.any { it.isDigit() }
            Timber.d("Customer ${customer.accountName}: mobile='${customer.mobile}', valid=$hasValidMobile")
            hasValidMobile
        }
        
        Timber.d("Filtered to ${validCustomers.size} valid customers")
        
        val total = validCustomers.size
        var sent = 0
        
        // Show initial progress
        withContext(Dispatchers.Main) {
            onProgress(0, total, total * 12)
        }
        
        try {
            for ((index, customer) in validCustomers.withIndex()) {
                try {
                    // Generate SMS message using template
                    val companyName = UserEnv.getCompanyName(context) ?: ""
                    val add1 = UserEnv.getAddress1(context) ?: ""
                    val add2 = UserEnv.getAddress2(context) ?: ""
                    val add3 = UserEnv.getAddress3(context) ?: ""
                    
                    val smsMessageRaw = template
                        .replace("{CmpName}", companyName)
                        .replace("{add1}", add1)
                        .replace("{add2}", add2)
                        .replace("{add3}", add3)
                        .replace("[customer]", customer.accountName)
                        .replace("[TM]", customer.balance)
                        .replace("[Mobile]", customer.mobile)
                        .ifBlank { 
                            "Dear ${customer.accountName}, your outstanding balance is ${customer.balance}. Please contact us for payment. - $companyName"
                        }
                    // Fallback: if template had placeholder lines like "**" (or blanks) for addresses, inject address1/2/3
                    val smsMessage = smsMessageRaw
                        .lines()
                        .mapIndexed { idx, line ->
                            when {
                                idx == 1 && (line.trim() == "**" || line.isBlank()) -> add1
                                idx == 2 && (line.trim() == "**" || line.isBlank()) -> add2
                                idx == 3 && (line.trim() == "**" || line.isBlank()) -> add3
                                else -> line
                            }
                        }
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    
                    // Clean phone number
                    val phoneNumber = customer.mobile.filter { it.isDigit() }
                    
                    // Send SMS using SmsManager
                    try {
                        val smsManager = SmsManager.getDefault()
                        
                        // Split long messages if needed
                        val messageParts = smsManager.divideMessage(smsMessage)
                        
                        if (messageParts.size == 1) {
                            smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null)
                        } else {
                            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null)
                        }
                        
                        Timber.d("SMS sent to ${customer.accountName} - $phoneNumber")
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "SMS sent to ${customer.accountName}", Toast.LENGTH_SHORT).show()
                        }
                        
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send SMS to ${customer.accountName}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to send SMS to ${customer.accountName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    sent++
                    val remaining = total - sent
                    val eta = remaining * 12 // Average 12 seconds per message
                    
                    withContext(Dispatchers.Main) {
                        onProgress(sent, total, eta)
                    }
                    
                    // Random delay between 8-15 seconds
                    if (index < validCustomers.size - 1) {
                        val delayMs = (8000..15000).random().toLong()
                        delay(delayMs)
                    }
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error sending SMS to ${customer.accountName}")
                    sent++
                    withContext(Dispatchers.Main) {
                        onProgress(sent, total, (total - sent) * 12)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Bulk SMS sending failed")
        } finally {
            withContext(Dispatchers.Main) {
                onComplete()
                Toast.makeText(
                    context,
                    "SMS sending completed. $sent/$total messages processed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * Send individual SMS to a single customer
 */
private fun sendSingleSMS(
    context: Context,
    customer: OutstandingEntry,
    template: String
) {
    try {
        // Generate SMS message using template
        val companyName = UserEnv.getCompanyName(context) ?: ""
        val add1 = UserEnv.getAddress1(context) ?: ""
        val add2 = UserEnv.getAddress2(context) ?: ""
        val add3 = UserEnv.getAddress3(context) ?: ""
        
        val smsMessageRaw = template
            .replace("{CmpName}", companyName)
            .replace("{add1}", add1)
            .replace("{add2}", add2)
            .replace("{add3}", add3)
            .replace("[customer]", customer.accountName)
            .replace("[TM]", customer.balance)
            .replace("[Mobile]", customer.mobile)
            .ifBlank { 
                "Dear ${customer.accountName}, your outstanding balance is ${customer.balance}. Please contact us for payment. - $companyName"
            }
        val smsMessage = smsMessageRaw
            .lines()
            .mapIndexed { idx, line ->
                when {
                    idx == 1 && (line.trim() == "**" || line.isBlank()) -> add1
                    idx == 2 && (line.trim() == "**" || line.isBlank()) -> add2
                    idx == 3 && (line.trim() == "**" || line.isBlank()) -> add3
                    else -> line
                }
            }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        
        // Clean phone number
        val phoneNumber = customer.mobile.filter { it.isDigit() }
        
        // Send SMS using Android Intent
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", smsMessage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        if (smsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(smsIntent)
            Toast.makeText(context, "SMS app opened for ${customer.accountName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No SMS app available", Toast.LENGTH_SHORT).show()
        }
        
    } catch (e: Exception) {
        Timber.e(e, "Failed to send SMS to ${customer.accountName}")
        Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
private fun OutstandingReportScreenPreview() {
    OutstandingReportScreenImpl()
}