package com.example.ui

import android.content.Context
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ContactRepository
import com.example.data.WhitelistContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

class MainViewModel(private val repository: ContactRepository) : ViewModel() {

    // List of whitelisted contacts
    val whitelistContacts: StateFlow<List<WhitelistContact>> = repository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Full device contacts loaded from phonebook
    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    // Loading indicator for device contacts
    private val _isDeviceContactsLoading = MutableStateFlow(false)
    val isDeviceContactsLoading: StateFlow<Boolean> = _isDeviceContactsLoading.asStateFlow()

    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val normalized = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    PhoneNumberUtils.normalizeNumber(phoneNumber) ?: phoneNumber
                } else {
                    phoneNumber.replace("[^0-9+]".toRegex(), "")
                }
            } catch (e: Exception) {
                phoneNumber
            }
            repository.insert(
                WhitelistContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    normalizedNumber = normalized
                )
            )
        }
    }

    fun toggleContactEnabled(contact: WhitelistContact) {
        viewModelScope.launch {
            repository.update(contact.copy(isEnabled = !contact.isEnabled))
        }
    }

    fun removeContact(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun loadPhonebookContacts(context: Context) {
        viewModelScope.launch {
            _isDeviceContactsLoading.value = true
            val list = mutableListOf<DeviceContact>()
            try {
                val hasContactsPermission = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasContactsPermission) {
                    val cursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                    )
                    cursor?.use {
                        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        
                        val seenNumbers = mutableSetOf<String>()
                        while (it.moveToNext()) {
                            val name = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                            val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                            
                            val cleanedNumber = number.replace("\\s+".toRegex(), "")
                            if (name.isNotEmpty() && cleanedNumber.isNotEmpty() && !seenNumbers.contains(cleanedNumber)) {
                                seenNumbers.add(cleanedNumber)
                                list.add(DeviceContact(name, number))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _deviceContacts.value = list
            _isDeviceContactsLoading.value = false
        }
    }

    // Factory to instantiate ViewModel with dependency
    class Factory(private val repository: ContactRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
