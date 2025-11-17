package com.example.childtrackerapp.schedule.model

data class DaySchedules(
    val date: String,
    val dayName: String,
    val schedules: List<Schedule>
)