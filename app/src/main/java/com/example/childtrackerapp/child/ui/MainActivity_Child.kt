package com.example.childtrackerapp.child.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.example.childtrackerapp.R
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.child.ui.screen.ChildMainScreen
import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.service.BlockerService
import com.example.childtrackerapp.service.LocationService
import com.example.childtrackerapp.service.NotificationPermissionActivity
import com.example.childtrackerapp.ui.theme.ChildTrackerTheme

import kotlin.getValue


class MainActivity_Child : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val childViewModel: ChildViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startLocationServiceIfPermitted()

        val intent = Intent(this, BlockerService::class.java)
        startService(intent)

        if (!ensureAccessibilityEnabled()) {
            showAccessibilityNotification()
        }

        if (!hasUsageStatsPermission()) {
            showUsageAccessNotification()
        }
        // Check và request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(this, NotificationPermissionActivity::class.java))
            }
        }

        setContent {
            ChildTrackerTheme {
                ChildMainScreen(authViewModel, childViewModel)
            }
        }
    }

    private fun ensureAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val myService = "com.example.childtrackerapp/.service.BlockedAppAccessibilityService"
        val isEnabled = enabledServices.contains(myService)

        if (!isEnabled) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        return isEnabled
    }

    private fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        val granted = !stats.isNullOrEmpty()
        return granted
    }

    private fun showAccessibilityNotification() {
        val channelId = "accessibility_channel"
        val channelName = "Accessibility Permission"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // icon app
            .setContentTitle("Accessibility Permission Needed")
            .setContentText("Tap to enable accessibility for child blocking")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun showUsageAccessNotification() {
        val channelId = "usage_channel"
        val channelName = "Usage Access Permission"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Usage Access Needed")
            .setContentText("Tap to allow app usage tracking for child blocking")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }


    private fun startLocationServiceIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    1001
                )
                return
            }
        }

        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

}
