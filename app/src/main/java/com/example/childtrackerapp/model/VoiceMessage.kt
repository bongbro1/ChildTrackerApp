package com.example.childtrackerapp.model

data class VoiceMessage(
    val id: String = "",              // ID của người gửi (ví dụ: "child1" hoặc "parent1")
    val text: String = "",            // Nội dung tin nhắn (text)
    val timestamp: Long = 0L,         // Thời gian gửi
    val from: String = "",            // Nguồn gửi ("child" hoặc "parent")
    val to: String = ""               //  Người nhận ("parent" hoặc "child")
)
