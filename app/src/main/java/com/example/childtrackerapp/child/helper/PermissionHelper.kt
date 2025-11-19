package com.example.childtrackerapp.child.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.R

object PermissionHelper {

    private const val PREFS = "app_prefs"

    fun showAccessibilityNotificationIfNeeded(context: Context) {
        val enabled = isAccessibilityEnabled(context)
        if (!enabled) {
            showNotification(
                context,
                1001,
                "Accessibility Needed",
                "Tap to enable accessibility",
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }
    }


    fun showUsageAccessNotificationIfNeeded(context: Context) {
        if (!isUsagePermissionGranted(context)) {
            showNotification(
                context,
                1002,
                "Usage Access Needed",
                "Tap to allow usage access",
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            )
        }
    }


    private fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${context.packageName}.service.BlockedAppAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        return enabledServices.contains(expected)
    }

    private fun isUsagePermissionGranted(context: Context): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 60000,
            System.currentTimeMillis()
        )
        return !stats.isNullOrEmpty()
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        intent: Intent
    ) {
        val channelId = "perm_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Permissions", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(id, notification)
    }

    private fun Context.getPrefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
