package com.example.childtrackerapp.worker

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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

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
            val existingPackages = existingSnapshot.children
                .mapNotNull { decodeKey(it.key!!) }
                .toSet()

            // ============================
            // 🔥 CẬP NHẬT usageTime cho app còn tồn tại
            // ============================
            existingSnapshot.children.forEach { childSnapshot ->
                val pkg = decodeKey(childSnapshot.key!!) ?: return@forEach
                val usageMs = usageMap[pkg]?.totalTimeInForeground ?: 0L

                val existingApp = childSnapshot.getValue(AppInfo::class.java)
                if (existingApp != null) {
                    val updatedApp = existingApp.copy(
                        usageTime = formatUsageTime(usageMs),
                    )
                    dbRef.child(childSnapshot.key!!).setValue(updatedApp).await()
                }
            }

            // ============================
            // 🔥 THÊM APP MỚI PHÁT HIỆN TRONG MÁY
            // ============================
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

            // Gửi app mới
            appsToSend.forEach { app ->
                val safeKey = encodeKey(app.packageName)
                dbRef.child(safeKey).setValue(app).await()
            }

            Log.d("SendAppsWorker", "Apps synced successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SendAppsWorker", "Failed: ${e.message}")
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