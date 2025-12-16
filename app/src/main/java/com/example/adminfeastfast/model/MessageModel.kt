package com.example.adminfeastfast.model

data class MessageModel(
    val message: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val sentByAdmin: Boolean = false, // Critical for distinguishing Admin vs User
    val timestamp: Long = 0
)
