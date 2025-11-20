package com.example.childtrackerapp.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.childtrackerapp.admin.MainActivity
import com.example.childtrackerapp.helpers.NotificationHelper
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AppStatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val role = inputData.getString("role") ?: return Result.failure()
        val childId = inputData.getString("childId") ?: return Result.failure()
        val childName = inputData.getString("childName") ?: return Result.failure()
        val packageName = "com_example_childtrackerapp"
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("blocked_items")
            .child(childId)
            .child("lastSeen")
            .child(packageName)

        return try {
            if (role == "con") {
                // Child: cập nhật timestamp
                dbRef.setValue(System.currentTimeMillis()).await()
            } else if (role == "cha") {
                // Parent: kiểm tra timestamp của child
                val snapshot = dbRef.get().await()
                val lastSeen = snapshot.getValue(Long::class.java) ?: return Result.success()

                // Nếu quá 10 phút chưa cập nhật
                if (System.currentTimeMillis() - lastSeen > 10 * 60 * 1000) {
                    // Đánh dấu app bị xóa
                    dbRef.setValue(true).await()

                    // Gửi notification cho parent
                    NotificationHelper.showNotification(
                        applicationContext,
                        2001,
                        "Child App đã bị xóa",
                        "$childName đã xóa Child App",
                        Intent(applicationContext, MainActivity::class.java),
                        NotificationHelper.IntentType.ACTIVITY
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
