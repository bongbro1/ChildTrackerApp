package com.example.childtrackerapp.parent.ui.model

data class ViolationLog(
    val packageName: String = "",
    val name: String = "",
    val violatedAt: String = "",
    val reason: String = "",
    val message: String = "",
    val notified: Boolean = false
)
