package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.repository.JivaRepository
import com.example.jiva.screens.CustomerContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class WhatsAppUiState(
    val customerContacts: List<CustomerContact> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class WhatsAppViewModel(
    private val jivaRepository: JivaRepository
) : ViewModel() {
    
    private val remote = com.example.jiva.data.network.RemoteDataSource()

    private val _uiState = MutableStateFlow(WhatsAppUiState(isLoading = true))
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()
    
    /**
     * Load customer contacts from Outstanding API with default filter: under = "Sundry debtors"
     */
    fun loadCustomerContactsFromOutstanding(userId: Int, year: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val filters = mapOf("under" to "Sundry debtors")
                val resp = remote.getOutstanding(userId, year, filters)
                if (resp.isSuccess) {
                    val data = resp.getOrNull()?.data.orEmpty()
                    val contacts = data
                        .filter { !it.mobile.isNullOrBlank() }
                        .map {
                            CustomerContact(
                                accountNumber = it.acId.ifBlank { "0" },
                                accountName = (it.accountName ?: ""),
                                mobileNumber = formatMobileNumber(it.mobile),
                                isSelected = false
                            )
                        }
                    _uiState.value = _uiState.value.copy(customerContacts = contacts, isLoading = false)
                    Timber.d("Loaded ${contacts.size} contacts from Outstanding API")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = resp.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading contacts from Outstanding API")
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    /**
     * Fallback: Loads customer contacts from the database (AccountMaster)
     */
    fun loadCustomerContacts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val accountsFlow = jivaRepository.getAllAccounts()
                accountsFlow.collect { accounts ->
                    val customerContacts = accounts
                        .filter { !it.mobile.isNullOrBlank() }
                        .map { account ->
                            CustomerContact(
                                accountNumber = account.acId?.toString() ?: "0",
                                accountName = account.accountName,
                                mobileNumber = formatMobileNumber(account.mobile ?: ""),
                                isSelected = false
                            )
                        }
                    _uiState.value = _uiState.value.copy(
                        customerContacts = customerContacts,
                        isLoading = false
                    )
                    Timber.d("Loaded ${customerContacts.size} customer contacts from DB")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading customer contacts from DB")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load customer contacts: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Format mobile number to include country code if not present
     */
    private fun formatMobileNumber(mobile: String): String {
        // For UI: show 10-digit local number without +91
        val digits = mobile.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }
    
    /**
     * Updates the selection state of a customer contact
     */
    fun updateContactSelection(accountNumber: String, isSelected: Boolean) {
        val currentContacts = _uiState.value.customerContacts.toMutableList()
        val index = currentContacts.indexOfFirst { it.accountNumber == accountNumber }
        if (index != -1) {
            currentContacts[index] = currentContacts[index].copy(isSelected = isSelected)
            _uiState.value = _uiState.value.copy(customerContacts = currentContacts)
        }
    }
    
    /**
     * Selects or deselects all customer contacts
     */
    fun selectAllContacts(selectAll: Boolean) {
        val updatedContacts = _uiState.value.customerContacts.map { it.copy(isSelected = selectAll) }
        _uiState.value = _uiState.value.copy(customerContacts = updatedContacts)
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}