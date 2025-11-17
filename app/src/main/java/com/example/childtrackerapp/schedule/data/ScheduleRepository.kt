package com.example.childtrackerapp.data.repository

import com.example.childtrackerapp.schedule.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getSchedulesForDate(date: String): Flow<List<Schedule>>
    fun getSchedulesForWeek(startDate: String): Flow<Map<String, List<Schedule>>>
    suspend fun addSchedule(schedule: Schedule)
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(id: String)
    fun getScheduleById(id: String): Flow<Schedule?>
    suspend fun addScheduleBatch(schedules: List<Schedule>)
    suspend fun initializeSampleData()
}