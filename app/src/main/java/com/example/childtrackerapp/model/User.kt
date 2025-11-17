package com.example.childtrackerapp.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val name: String = "",
    val parentId: String? = null
)
