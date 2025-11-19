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
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.child.ui.BlockedAppActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BlockedAppAccessibilityService : AccessibilityService() {

    // Danh sách app bị block, có thể update từ Firebase
    private var blockedApps = emptyMap<String, String>()
    private lateinit var sessionManager: SessionManager
    private val serviceJob = SupervisorJob()
    private var sessionActive = true

    override fun onServiceConnected() {
        super.onServiceConnected()

        sessionManager = SessionManager(applicationContext)
        sessionActive = sessionManager.isLoggedIn()

        val sessionManager = SessionManager(applicationContext)
        val childId = sessionManager.getUserId()
        if (childId.isNullOrEmpty()) {
            Log.e("AccessibilityService", "Child ID not found")
            return
        }

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("blocked_items")
            .child(childId)
            .child("apps")

        // Lắng nghe dữ liệu realtime
        dbRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                blockedApps = snapshot.children
                    .filter { it.child("allowed").getValue(Boolean::class.java) == false }
                    .mapNotNull {
                        val pkg = it.child("packageName").getValue(String::class.java)
                        val name = it.child("name").getValue(String::class.java)
                        if (pkg != null && name != null) pkg to name else null
                    }.toMap()

                Log.d("AccessibilityService", "Blocked apps updated: $blockedApps")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AccessibilityService", "Failed to fetch blocked apps: ${error.message}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }



    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!sessionActive) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d("AccessibilityService", "Current app: $packageName")

            if (blockedApps.containsKey(packageName)) {
                Log.d("AccessibilityService", "Blocked app detected: $packageName")

                val appName = blockedApps[packageName] ?: packageName
                performGlobalAction(GLOBAL_ACTION_HOME)
                Handler(Looper.getMainLooper()).postDelayed({
                    showBlockedAppNotification(appName)
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
