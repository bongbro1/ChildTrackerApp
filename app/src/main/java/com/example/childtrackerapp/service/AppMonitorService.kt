package com.example.childtrackerapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.R
import com.example.childtrackerapp.admin.MainActivity
import com.example.childtrackerapp.child.helper.decodeKey
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.example.childtrackerapp.parent.ui.model.ViolationLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

class AppMonitorService : Service() {

    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private var childId: String = "";
    private val firebaseDb = FirebaseDatabase.getInstance().reference
    private val lastViolationState = mutableMapOf<String, Boolean>()
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val configs = CopyOnWriteArrayList<AppInfo>()

    override fun onCreate() {
        super.onCreate()

        // 1. Khởi tạo sessionManager
        sessionManager = SessionManager(applicationContext)

        // 2. Lấy childId
        childId = sessionManager.getUserId() ?: "unknown_child"

        // 3. Start foreground service & monitoring
        createNotificationChannel()
        startForegroundService()
        startMonitoring()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "monitor_channel",
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java) // activity mở khi click
        val notification = NotificationCompat.Builder(this, "perm_channel")
            .setContentTitle("Đang giám sát ứng dụng")
            .setContentText("Điện thoại của bạn đang được giám sát")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(PendingIntent.getActivity(
                this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            .build()
        startForeground(1, notification)
    }

    private fun startMonitoring() {

        // Lắng nghe configs từ Firebase
        firebaseDb.child("blocked_items/$childId/apps")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    configs.clear()
                    snapshot.children.forEach { childSnap ->
                        childSnap.getValue(AppInfo::class.java)?.let {
                            configs.add(it)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppMonitor", "Firebase listener cancelled", error.toException())
                }
            })

        // Loop check foreground mỗi 10s
        monitoringScope.launch {
            while (isActive) {
                try {
                    val pkg = getForegroundApp()

                    if (pkg != null) {
                        // copyOnWriteArrayList cho phép forEach an toàn
                        configs.forEach { config ->
                            if (decodeKey(config.packageName) == pkg) {
                                checkForegroundAndSendViolation(config)
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("AppMonitor", "Error in monitoring loop", e)
                }

                delay(10_000)
            }
        }
    }
    private fun checkForegroundAndSendViolation(config: AppInfo) {
        try {
            val pkg = getForegroundApp() ?: return
//            Log.d("AppMonitor", "pkg $pkg")
            if (pkg == decodeKey(config.packageName)) {
                val allowedNow = isAllowed(config)
                val wasViolated = lastViolationState[pkg] ?: false

                if (!allowedNow && !wasViolated) {
                    lastViolationState[pkg] = true
                    sendViolation(config)
                } else if (allowedNow) {
                    lastViolationState[pkg] = false
                }
            }
        } catch (e: Exception) {
            Log.e("AppMonitor", "Error checking foreground app", e)
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 1000 * 10
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        if (stats.isNullOrEmpty()) return null
        val recent = stats.maxByOrNull { it.lastTimeUsed }
        return recent?.packageName
    }

    private fun isAllowed(config: AppInfo): Boolean {
        if (!config.allowed) {
            return false
        }
        // check day
        val today = LocalDate.now().dayOfWeek.name.take(3).lowercase()
        val allowedDays = config.allowedDays.map { it.lowercase() }
        if (!allowedDays.contains(today)) {
            return false
        }
        // check time
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalTime.now()
        val start = LocalTime.parse(config.startTime.trim(), formatter)
        val end = LocalTime.parse(config.endTime.trim(), formatter)

        val allowedNow = if (start <= end) {
            now >= start && now <= end
        } else {
            now >= start || now <= end
        }

        return allowedNow
    }


    private fun sendViolation(config: AppInfo) {
        val logId = firebaseDb.push().key ?: "${System.currentTimeMillis()}"
        val log = ViolationLog(
            packageName = config.packageName,
            name = config.name,
            violatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()),
            reason = "Used outside allowed time",
            message = "Child mở ${config.name} sai thời gian"
        )

        firebaseDb.child("blocked_items/$childId/notification_logs/$logId")
            .setValue(log)
            .addOnSuccessListener {
                Log.d("AppMonitor", "Sending violation for package: ${config.packageName}")
            }
            .addOnFailureListener { e ->
                Log.e("AppMonitor", "Failed to send violation", e)
            }
    }

    override fun onBind(intent: Intent?) = null
}
