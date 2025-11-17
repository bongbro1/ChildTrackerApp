package com.example.childtrackerapp.schedule.model

data class Schedule(
    val id: String,
    val title: String,
    val startTime: String, // Format: "HH:mm"
    val endTime: String,   // Format: "HH:mm"
    val description: String,
    val location: String,
    val date: String       // Format: "yyyy-MM-dd"
)