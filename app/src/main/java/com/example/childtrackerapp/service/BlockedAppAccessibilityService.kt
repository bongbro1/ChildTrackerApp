package com.example.childtrackerapp.service


import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.childtrackerapp.R
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.child.ui.BlockedAppActivity

class BlockedAppAccessibilityService : AccessibilityService() {

    // Danh sách app bị block, có thể update từ Firebase
    private val blockedApps = listOf("com.zing.zalo", "com.samsung.android.calendar")

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d("AccessibilityService", "Current app: $packageName")

            if (blockedApps.contains(packageName)) {
                Log.d("AccessibilityService", "Blocked app detected: $packageName")

                // Mở Activity cảnh báo
//                val intent = Intent(this, BlockedAppActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.putExtra("appName", packageName)
//                startActivity(intent)

                // Quay về Home để chặn app
                performGlobalAction(GLOBAL_ACTION_HOME)
                Handler(Looper.getMainLooper()).postDelayed({
                    showBlockedAppNotification(packageName)
                }, 300)
            }
        }
    }
    private fun showBlockedAppNotification(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("BlockedAppNotification", "Notification permission not granted!")
                return
            }
        }

        // code notification như cũ
        val channelId = "blocked_app_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Blocked Apps",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("appName", packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // icon test chắc chắn hiển thị
            .setContentTitle("Blocked App")
            .setContentText("$packageName is blocked")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }



    override fun onInterrupt() {
        // Required override, có thể để trống
    }
}
