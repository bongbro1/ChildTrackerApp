package com.example.childtrackerapp.schedule.navigation

sealed class Destinations(val route: String) {
    object Daily : Destinations("daily?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "daily?date=$date" else "daily"
    }
    object Weekly : Destinations("weekly")
    object AddSchedule : Destinations("add_schedule/{date}") {
        fun createRoute(date: String) = "add_schedule/$date"
    }
    object EditSchedule : Destinations("edit_schedule/{scheduleId}") {
        fun createRoute(scheduleId: String) = "edit_schedule/$scheduleId"
    }
}

