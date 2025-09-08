package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.repository.JivaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class DayEndReportUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val companyCode: Int = 1,
    val currentDate: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val yearString: String = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()),

    // DayEndInfo API state
    val isDayEndLoading: Boolean = false,
    val selectedDayDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val dayEndPurchase: Double = 0.0,
    val dayEndSale: Double = 0.0,
    val dayEndCashOpening: Double = 0.0,
    val dayEndCashClosing: Double = 0.0,
    val dayEndBankOpening: Double = 0.0,
    val dayEndBankClosing: Double = 0.0,
    val dayEndExpenses: Double = 0.0,
    val dayEndPayments: Double = 0.0,
    val dayEndReceipts: Double = 0.0,
    
    // Summary metrics
    val totalAccounts: Int = 0,
    val totalBalance: Double = 0.0,
    val accountsOverCreditLimit: Int = 0,
    val inactiveAccounts: Int = 0,
    
    // Business analysis
    val averageBalancePerAccount: Double = 0.0,
    val averageDaysSinceLastTransaction: Double = 0.0,
    val percentAccountsWithinCreditLimit: Double = 0.0,
    
    // Performance indicators
    val balanceByUnderType: Map<String, Double> = emptyMap(),
    val accountsByCreditStatus: Map<String, Int> = emptyMap(),
    
    // Account list
    val accounts: List<AccountSummary> = emptyList()
)

data class AccountSummary(
    val accountId: Int,
    val accountName: String,
    val balance: Double,
    val lastTransactionDate: String,
    val daysSinceLastTransaction: Int,
    val isOverCreditLimit: Boolean,
    val creditLimitAmount: Double,
    val creditLimitDays: Int
)

class DayEndReportViewModel(
    private val jivaRepository: JivaRepository
) : ViewModel() {
    
    private val remoteDataSource = com.example.jiva.data.network.RemoteDataSource()
    private val _uiState = MutableStateFlow(DayEndReportUiState())
    val uiState: StateFlow<DayEndReportUiState> = _uiState.asStateFlow()
    
    init {
        loadDayEndReport()
    }
    
    fun loadDayEndReport() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Get all closing balances
                val closingBalances = jivaRepository.getAllClosingBalances().first()
                
                if (closingBalances.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No closing balance data found"
                    )
                    return@launch
                }
                
                // Calculate summary metrics
                val totalAccounts = closingBalances.size
                
                val totalBalance = closingBalances
                    .mapNotNull { it.balance?.toDoubleOrNull() }
                    .sum()
                
                val accountsOverCreditLimit = closingBalances.count { 
                    val balance = it.balance?.toDoubleOrNull() ?: 0.0
                    val creditLimit = it.creditLimitAmount?.toDoubleOrNull() ?: 0.0
                    balance > creditLimit && creditLimit > 0
                }
                
                val inactiveAccounts = closingBalances.count { 
                    val days = it.days
                    val creditLimitDays = it.creditLimitDays?.toIntOrNull() ?: 0
                    days > creditLimitDays && creditLimitDays > 0
                }
                
                // Calculate business analysis
                val averageBalancePerAccount = if (totalAccounts > 0) totalBalance / totalAccounts else 0.0
                
                val averageDaysSinceLastTransaction = if (totalAccounts > 0) {
                    closingBalances.sumOf { it.days.toDouble() } / totalAccounts
                } else 0.0
                
                val accountsWithinCreditLimit = totalAccounts - accountsOverCreditLimit
                val percentAccountsWithinCreditLimit = if (totalAccounts > 0) {
                    (accountsWithinCreditLimit.toDouble() / totalAccounts) * 100
                } else 0.0
                
                // Calculate performance indicators
                val balanceByUnderType = closingBalances
                    .filter { !it.under.isNullOrBlank() }
                    .groupBy { it.under!! }
                    .mapValues { (_, accounts) -> 
                        accounts.sumOf { it.balance?.toDoubleOrNull() ?: 0.0 }
                    }
                
                val accountsByCreditStatus = mapOf(
                    "Within Limit" to accountsWithinCreditLimit,
                    "Over Limit" to accountsOverCreditLimit
                )
                
                // Create account summaries
                val accountSummaries = closingBalances.map { entity ->
                    val balance = entity.balance?.toDoubleOrNull() ?: 0.0
                    val creditLimit = entity.creditLimitAmount?.toDoubleOrNull() ?: 0.0
                    val creditLimitDays = entity.creditLimitDays?.toIntOrNull() ?: 0
                    
                    AccountSummary(
                        accountId = entity.acId,
                        accountName = entity.accountName,
                        balance = balance,
                        lastTransactionDate = entity.lastDate ?: "N/A",
                        daysSinceLastTransaction = entity.days,
                        isOverCreditLimit = balance > creditLimit && creditLimit > 0,
                        creditLimitAmount = creditLimit,
                        creditLimitDays = creditLimitDays
                    )
                }
                
                // Get company code and year string from the first record
                val companyCode = closingBalances.firstOrNull()?.cmpCode ?: 1
                val yearString = closingBalances.firstOrNull()?.yearString ?: 
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    companyCode = companyCode,
                    yearString = yearString,
                    totalAccounts = totalAccounts,
                    totalBalance = totalBalance,
                    accountsOverCreditLimit = accountsOverCreditLimit,
                    inactiveAccounts = inactiveAccounts,
                    averageBalancePerAccount = averageBalancePerAccount,
                    averageDaysSinceLastTransaction = averageDaysSinceLastTransaction,
                    percentAccountsWithinCreditLimit = percentAccountsWithinCreditLimit,
                    balanceByUnderType = balanceByUnderType,
                    accountsByCreditStatus = accountsByCreditStatus,
                    accounts = accountSummaries
                )
                
                Timber.d("Day End Report loaded successfully with ${accountSummaries.size} accounts")
                
            } catch (e: Exception) {
                Timber.e(e, "Error loading day end report")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading report: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updateSelectedDate(dateIso: String) {
        _uiState.value = _uiState.value.copy(selectedDayDate = dateIso)
    }

    /**
     * Fetch DayEndInfo using userID from env (stored as string) and selected ISO date (yyyy-MM-dd)
     */
    fun fetchDayEndInfo(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDayEndLoading = true, errorMessage = null)

                val userIdStr = com.example.jiva.utils.UserEnv.getUserId(context).orEmpty()
                val userId = userIdStr.toIntOrNull()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isDayEndLoading = false, errorMessage = "User not logged in")
                    return@launch
                }

                val dateIso = _uiState.value.selectedDayDate // yyyy-MM-dd

                val result = remoteDataSource.getDayEndInfo(cmpCode = userId, dayDate = dateIso)
                if (result.isSuccess) {
                    val body = result.getOrNull()
                    if (body?.isSuccess == true && body.data != null) {
                        val d = body.data
                        fun asDouble(s: String?): Double = s?.toDoubleOrNull() ?: 0.0

                        _uiState.value = _uiState.value.copy(
                            isDayEndLoading = false,
                            companyCode = d.cmpCode?.toIntOrNull() ?: userId,
                            dayEndPurchase = asDouble(d.purchase),
                            dayEndSale = asDouble(d.sale),
                            dayEndCashOpening = asDouble(d.cashOpening),
                            dayEndCashClosing = asDouble(d.cashClosing),
                            dayEndBankOpening = asDouble(d.bankOpening),
                            dayEndBankClosing = asDouble(d.bankClosing),
                            dayEndExpenses = asDouble(d.expenses),
                            dayEndPayments = asDouble(d.payments),
                            dayEndReceipts = asDouble(d.receipts)
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isDayEndLoading = false, errorMessage = body?.message ?: "Failed to load Day End info")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isDayEndLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching DayEndInfo")
                _uiState.value = _uiState.value.copy(isDayEndLoading = false, errorMessage = e.message)
            }
        }
    }
}