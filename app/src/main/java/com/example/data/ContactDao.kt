package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM whitelist_contacts ORDER BY timestamp DESC")
    fun getAllContacts(): Flow<List<WhitelistContact>>

    @Query("SELECT * FROM whitelist_contacts WHERE isEnabled = 1")
    suspend fun getEnabledContactsList(): List<WhitelistContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: WhitelistContact)

    @Update
    suspend fun updateContact(contact: WhitelistContact)

    @Delete
    suspend fun deleteContact(contact: WhitelistContact)

    @Query("DELETE FROM whitelist_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)
}
