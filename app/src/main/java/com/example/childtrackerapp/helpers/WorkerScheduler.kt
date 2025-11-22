package com.example.childtrackerapp.helpers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.worker.AppStatusWorker
import com.example.childtrackerapp.worker.AppsWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkerScheduler @Inject constructor(
    private val sessionManager: SessionManager
) {

    fun scheduleSendAppsWorker(context: Context) {
        val childId = sessionManager.getUserId() ?: return

        // 1. Chạy ngay lần đầu
        val immediateWork = OneTimeWorkRequestBuilder<AppsWorker>()
            .setInputData(workDataOf("childId" to childId))
            .build()
        WorkManager.getInstance(context).enqueue(immediateWork)

        // 2. Lên lịch định kỳ
        val periodicWork = PeriodicWorkRequestBuilder<AppsWorker>(1, TimeUnit.HOURS)
            .setInputData(workDataOf("childId" to childId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "send_apps_worker_$childId",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )
    }

    fun scheduleAppStatusWorker(context: Context) {
        val childId = sessionManager.getUserId() ?: return
        val role = sessionManager.getRole() ?: return
        val childName = sessionManager.getUserName() ?: "Con của bạn"

        val inputData = workDataOf(
            "childId" to childId,
            "role" to role,
            "childName" to childName
        )

        // 1. Chạy ngay lần đầu
        val immediateWork = OneTimeWorkRequestBuilder<AppStatusWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueue(immediateWork)

        // 2. Lên lịch định kỳ
        val periodicWork = PeriodicWorkRequestBuilder<AppStatusWorker>(5, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "app_status_worker_$childId",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )
    }
}