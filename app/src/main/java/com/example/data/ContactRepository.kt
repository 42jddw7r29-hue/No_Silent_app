package com.example.data

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
    val allContacts: Flow<List<WhitelistContact>> = contactDao.getAllContacts()

    suspend fun getEnabledContacts(): List<WhitelistContact> {
        return contactDao.getEnabledContactsList()
    }

    suspend fun insert(contact: WhitelistContact) {
        contactDao.insertContact(contact)
    }

    suspend fun update(contact: WhitelistContact) {
        contactDao.updateContact(contact)
    }

    suspend fun delete(contact: WhitelistContact) {
        contactDao.deleteContact(contact)
    }

    suspend fun deleteById(id: Int) {
        contactDao.deleteContactById(id)
    }
}
