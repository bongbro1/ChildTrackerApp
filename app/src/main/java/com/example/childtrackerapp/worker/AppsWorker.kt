package com.example.childtrackerapp.worker

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class AppsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val childId = inputData.getString("childId") ?: return Result.failure()

        val pm = applicationContext.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val userApps = installedApps.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000L
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        val usageMap = usageStatsList.associateBy { it.packageName }

        return try {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("blocked_items")
                .child(childId)
                .child("apps")

            // 1. Lấy danh sách hiện tại từ Firebase
            val existingSnapshot = dbRef.get().await()
            val existingPackages = existingSnapshot.children.mapNotNull { it.key }.toSet()

            // 2. Tạo danh sách AppInfo mới (chỉ app chưa có trong Firebase)
            val appsToSend = userApps.mapNotNull { appInfo ->
                if (existingPackages.contains(appInfo.packageName)) return@mapNotNull null

                val usageMs = usageMap[appInfo.packageName]?.totalTimeInForeground ?: 0L
                val iconDrawable = appInfo.loadIcon(pm)
                val iconBitmap = if (iconDrawable is BitmapDrawable) {
                    iconDrawable.bitmap
                } else {
                    val bitmap = Bitmap.createBitmap(
                        iconDrawable.intrinsicWidth.coerceAtLeast(1),
                        iconDrawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    iconDrawable.draw(canvas)
                    bitmap
                }

                AppInfo(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = appInfo.packageName,
                    iconBase64 = bitmapToBase64(iconBitmap),
                    isAllowed = true,
                    usageTime = formatUsageTime(usageMs)
                )
            }

            // 3. Ghi từng app mới vào Firebase (không overwrite)
            appsToSend.forEach { app ->
                dbRef.child(app.packageName).setValue(app).await()
            }

            Log.d("SendAppsWorker", "Apps sent successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SendAppsWorker", "Failed to send apps: ${e.message}")
            Result.retry()
        }
    }
    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun formatUsageTime(ms: Long): String {
        val minutes = (ms / 1000 / 60) % 60
        val hours = (ms / 1000 / 60 / 60)
        return "${hours}h ${minutes}m"
    }

}