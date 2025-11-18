package com.example.childtrackerapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.child.ui.BlockedAppActivity
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class BlockerService : Service() {

    private val TAG = "ChildBlocker"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created, starting blocking loop")

        val sessionManager = SessionManager(applicationContext)
        val childId = sessionManager.getUserId()
        if (childId.isNullOrEmpty()) {
            Log.e("BlockerService", "Child ID not found, cannot fetch blocked apps")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val blockedAppsFromFirebase = fetchBlockedApps(childId)
            if (blockedAppsFromFirebase.isNotEmpty()) {
                startBlockingLoop(blockedAppsFromFirebase)
            }
        }
    }
    private suspend fun fetchBlockedApps(childId: String): List<String> {
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("blocked_items")
            .child(childId)
            .child("apps")

        return try {
            val snapshot = dbRef.get().await()
            val blockedApps = snapshot.children
                .filter { it.child("allowed").getValue(Boolean::class.java) == false }
                .mapNotNull { it.child("packageName").getValue(String::class.java) }

            Log.d("BlockerService", "Fetched blocked apps for childId $childId: $blockedApps")
            blockedApps
        } catch (e: Exception) {
            Log.e("BlockerService", "Error fetching blocked apps: ${e.message}")
            emptyList()
        }
    }

    private fun startBlockingLoop(blocked: List<String>) {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                try {
                    checkForegroundApp(blocked)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in checkForegroundApp: ${e.message}")
                }
                delay(1500)
            }
        }
    }

    private fun checkForegroundApp(blocked: List<String>) {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 5000 // 5 giây trước
        val events = usageStatsManager.queryEvents(start, end)

        var lastApp: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = event.packageName
                Log.d(TAG, "Detected MOVE_TO_FOREGROUND: ${event.packageName} at ${event.timeStamp}")
            }
        }

        lastApp?.let { currentApp ->
            if (blocked.contains(currentApp)) {
                // Mở activity cảnh báo
                val intent = Intent(applicationContext, BlockedAppActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("appName", currentApp)
                startActivity(intent)

                // Kill background process
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(currentApp)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        val granted = !stats.isNullOrEmpty()
        return granted
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
