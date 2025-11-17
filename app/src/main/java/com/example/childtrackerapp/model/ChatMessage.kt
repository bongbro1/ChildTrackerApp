package com.example.childtrackerapp.model

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val receiver: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
