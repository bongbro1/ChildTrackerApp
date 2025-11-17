package com.example.childtrackerapp.schedule.data

import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.Schedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : ScheduleRepository {

    override fun getSchedulesForDate(date: String): Flow<List<Schedule>> {
        return firebaseDataSource.getSchedulesForDate(date)
    }

    override fun getSchedulesForWeek(startDate: String): Flow<Map<String, List<Schedule>>> {
        return firebaseDataSource.getSchedulesForWeek(startDate)
    }

    override suspend fun addSchedule(schedule: Schedule) {
        firebaseDataSource.addSchedule(schedule)
    }

    override suspend fun updateSchedule(schedule: Schedule) {
        firebaseDataSource.updateSchedule(schedule)
    }

    override suspend fun deleteSchedule(id: String) {
        firebaseDataSource.deleteSchedule(id)
    }

    override fun getScheduleById(id: String): Flow<Schedule?> {
        return firebaseDataSource.getScheduleById(id)
    }

    override suspend fun addScheduleBatch(schedules: List<Schedule>) {
        firebaseDataSource.addScheduleBatch(schedules)
    }

    override suspend fun initializeSampleData() {
        firebaseDataSource.initializeSampleData()
    }
}
