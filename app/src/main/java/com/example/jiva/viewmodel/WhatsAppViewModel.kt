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
    
    private val _uiState = MutableStateFlow(WhatsAppUiState(isLoading = true))
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()
    
    init {
        loadCustomerContacts()
    }
    
    /**
     * Loads customer contacts from the database
     * Fetches account names and mobile numbers from AccountMaster table
     */
    fun loadCustomerContacts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Get all accounts from the repository
                val accountsFlow = jivaRepository.getAllAccounts()
                
                // Collect the accounts and map them to CustomerContact objects
                accountsFlow.collect { accounts ->
                    val customerContacts = accounts
                        .filter { !it.mobile.isNullOrBlank() } // Only include accounts with mobile numbers
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
                    
                    Timber.d("Loaded ${customerContacts.size} customer contacts")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading customer contacts")
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
        // If mobile number doesn't start with +91, add it
        return if (mobile.startsWith("+91")) {
            mobile
        } else {
            "+91 $mobile"
        }
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
        val updatedContacts = _uiState.value.customerContacts.map { 
            it.copy(isSelected = selectAll) 
        }
        _uiState.value = _uiState.value.copy(customerContacts = updatedContacts)
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}