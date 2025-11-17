// data/datasource/FirebaseDataSource.kt
package com.example.childtrackerapp.schedule.data

import com.example.childtrackerapp.schedule.model.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseDataSource - Quản lý data từ Firebase Firestore
 * Collection: schedules
 * Document structure: {
 *   id: String,
 *   title: String,
 *   startTime: String,
 *   endTime: String,
 *   description: String,
 *   location: String,
 *   date: String
 * }
 */
@Singleton
class FirebaseDataSource @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val schedulesCollection = firestore.collection("schedules")
    
    /**
     * Lấy danh sách lịch trình cho một ngày cụ thể (realtime)
     */
    fun getSchedulesForDate(date: String): Flow<List<Schedule>> = callbackFlow {
        val listener = schedulesCollection
            .whereEqualTo("date", date)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val schedules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Schedule(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            date = doc.getString("date") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(schedules)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Lấy danh sách lịch trình cho một tuần (realtime)
     */
    fun getSchedulesForWeek(startDate: String): Flow<Map<String, List<Schedule>>> = callbackFlow {
        val weekDates = getWeekDates(startDate)
        val startDateObj = LocalDate.parse(startDate)
        val endDateObj = startDateObj.plusDays(6)
        
        val listener = schedulesCollection
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .orderBy("date", Query.Direction.ASCENDING)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val allSchedules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Schedule(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            date = doc.getString("date") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Nhóm schedules theo ngày
                val weekSchedules = weekDates.associateWith { date ->
                    allSchedules.filter { it.date == date }
                }
                
                trySend(weekSchedules)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Thêm lịch trình mới
     */
    suspend fun addSchedule(schedule: Schedule) {
        val scheduleData = hashMapOf(
            "title" to schedule.title,
            "startTime" to schedule.startTime,
            "endTime" to schedule.endTime,
            "description" to schedule.description,
            "location" to schedule.location,
            "date" to schedule.date,
            "createdAt" to System.currentTimeMillis()
        )
        
        if (schedule.id.isEmpty()) {
            // Firestore tự sinh ID
            schedulesCollection.add(scheduleData).await()
        } else {
            // Sử dụng ID được chỉ định
            schedulesCollection.document(schedule.id).set(scheduleData).await()
        }
    }
    
    /**
     * Cập nhật lịch trình
     */
    suspend fun updateSchedule(schedule: Schedule) {
        val scheduleData = hashMapOf(
            "title" to schedule.title,
            "startTime" to schedule.startTime,
            "endTime" to schedule.endTime,
            "description" to schedule.description,
            "location" to schedule.location,
            "date" to schedule.date,
            "updatedAt" to System.currentTimeMillis()
        )
        
        schedulesCollection.document(schedule.id).update(scheduleData as Map<String, Any>).await()
    }
    
    /**
     * Xóa lịch trình
     */
    suspend fun deleteSchedule(id: String) {
        schedulesCollection.document(id).delete().await()
    }
    
    /**
     * Lấy thông tin một lịch trình theo ID (realtime)
     */
    fun getScheduleById(id: String): Flow<Schedule?> = callbackFlow {
        val listener = schedulesCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val schedule = snapshot?.let { doc ->
                    if (doc.exists()) {
                        try {
                            Schedule(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                startTime = doc.getString("startTime") ?: "",
                                endTime = doc.getString("endTime") ?: "",
                                description = doc.getString("description") ?: "",
                                location = doc.getString("location") ?: "",
                                date = doc.getString("date") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
                
                trySend(schedule)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Thêm nhiều lịch trình cùng lúc (batch operation)
     * Hữu ích cho import từ Excel
     */
    suspend fun addScheduleBatch(schedules: List<Schedule>) {
        val batch = firestore.batch()
        
        schedules.forEach { schedule ->
            val scheduleData = hashMapOf(
                "title" to schedule.title,
                "startTime" to schedule.startTime,
                "endTime" to schedule.endTime,
                "description" to schedule.description,
                "location" to schedule.location,
                "date" to schedule.date,
                "createdAt" to System.currentTimeMillis()
            )
            
            val docRef = if (schedule.id.isEmpty()) {
                schedulesCollection.document()
            } else {
                schedulesCollection.document(schedule.id)
            }
            
            batch.set(docRef, scheduleData)
        }
        
        batch.commit().await()
    }
    
    /**
     * Tạo dữ liệu mẫu ban đầu (chỉ gọi một lần khi khởi tạo app)
     */
    suspend fun initializeSampleData() {
        // Kiểm tra xem đã có data chưa
        val existingSchedules = schedulesCollection.limit(1).get().await()
        if (!existingSchedules.isEmpty) {
            return // Đã có dữ liệu rồi
        }
        
        // Thêm dữ liệu mẫu
        val sampleSchedules = listOf(
            Schedule(
                id = "",
                title = "Đi học",
                startTime = "07:00",
                endTime = "10:30",
                description = "Ôn bài, học trên lớp",
                location = "Trường Tiểu học ABC",
                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ),
            Schedule(
                id = "",
                title = "Học thêm Toán",
                startTime = "14:00",
                endTime = "16:00",
                description = "Luyện tập bài tập nâng cao",
                location = "Trung tâm gia sư XYZ",
                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ),
            Schedule(
                id = "",
                title = "Học Tiếng Anh",
                startTime = "18:00",
                endTime = "19:30",
                description = "Lớp giao tiếp tiếng Anh",
                location = "Trung tâm ngoại ngữ DEF",
                date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        )
        
        addScheduleBatch(sampleSchedules)
    }
    
    /**
     * Helper function: Lấy danh sách 7 ngày từ thứ 2 đến chủ nhật
     */
    private fun getWeekDates(startDate: String): List<String> {
        val start = LocalDate.parse(startDate)
        return (0..6).map { 
            start.plusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
}
