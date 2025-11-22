package com.example.childtrackerapp.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.childtrackerapp.child.helper.decodeKey
import com.example.childtrackerapp.child.helper.encodeKey
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.example.childtrackerapp.utils.WEEK_DAYS
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val childId = inputData.getString("childId") ?: return Result.failure()

        val pm = applicationContext.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null)
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val includePackages = listOf("com.google.android.youtube", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

        val userApps = (installedApps.filter { appInfo ->
            ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) ||
                    includePackages.contains(appInfo.packageName)
        }).distinctBy { it.packageName }

//        Log.d("UserAppsDebug", "===== Final user apps list =====")
//        userApps.forEach { app ->
//            Log.d("UserAppsDebug", "UserApp: ${app.packageName} - ${app.loadLabel(pm)}")
//        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val usageMap = getUsageToday(usageStatsManager, startTime, endTime);

        return try {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("blocked_items")
                .child(childId)
                .child("apps")

            // 1. Lấy danh sách hiện tại từ Firebase
            val existingSnapshot = dbRef.get().await()
            val existingPackages = existingSnapshot.children
                .mapNotNull { decodeKey(it.key!!) }
                .toSet()

            // ============================
            // 🔥 CẬP NHẬT usageTime cho app còn tồn tại
            // ============================
            existingSnapshot.children.forEach { childSnapshot ->
                val pkg = decodeKey(childSnapshot.key!!) ?: return@forEach
                val usageMs = usageMap[pkg] ?: 0L
                val usageStr = formatUsageTime(usageMs)

                val existingApp = childSnapshot.getValue(AppInfo::class.java)
                if (existingApp != null) {
                    val updatedApp = existingApp.copy(
                        usageTime = usageStr,
                    )
                    dbRef.child(childSnapshot.key!!).setValue(updatedApp).await()

                    dbRef.child(childSnapshot.key!!)
                        .child("usage")
                        .child(today)
                        .setValue(usageStr)
                        .await()
                }
            }

            // ============================
            // 🔥 THÊM APP MỚI PHÁT HIỆN TRONG MÁY
            // ============================
            val appsToSend = userApps.mapNotNull { appInfo ->
                if (existingPackages.contains(appInfo.packageName)) return@mapNotNull null

                val usageMs = usageMap[appInfo.packageName] ?: 0L
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
                    allowed = true,
                    usageTime = formatUsageTime(usageMs),
                    startTime = "00:00",
                    endTime = "23:59",
                    allowedDays = WEEK_DAYS
                )
            }

            // Gửi app mới
            appsToSend.forEach { app ->
                val safeKey = encodeKey(app.packageName)
                dbRef.child(safeKey).setValue(app).await()

                dbRef.child(safeKey)
                    .child("usage")
                    .child(today)
                    .setValue(app.usageTime)
                    .await()
            }

            Log.d("SendAppsWorker", "Apps synced successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SendAppsWorker", "Failed: ${e.message}")
            Result.retry()
        }
    }
    private fun getUsageToday(usageStatsManager: UsageStatsManager, startTime: Long, endTime: Long): Map<String, Long> {
        val usageMap = mutableMapOf<String, Long>()
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        // Lưu thời gian foreground cuối cùng cho mỗi app
        val lastForegroundMap = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Lưu lại timestamp khi app chuyển foreground
                    lastForegroundMap[packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val lastStart = lastForegroundMap[packageName] ?: continue
                    val duration = event.timeStamp - lastStart
                    usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                    lastForegroundMap.remove(packageName) // reset
                }
            }
        }

        return usageMap // Map<packageName, totalMsToday>
    }
    fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun formatUsageTime(ms: Long): String {
        val totalMinutes = ms / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

}