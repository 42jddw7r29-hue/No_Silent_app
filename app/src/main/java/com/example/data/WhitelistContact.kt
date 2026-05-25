package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_contacts")
data class WhitelistContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
