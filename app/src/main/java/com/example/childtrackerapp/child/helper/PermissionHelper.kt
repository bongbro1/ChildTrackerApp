package com.example.childtrackerapp.child.helper

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.childtrackerapp.helpers.NotificationHelper

object PermissionHelper {
    private const val PREFS = "app_prefs"

    fun showUsageAccessNotificationIfNeeded(context: Context) {
        if (!isUsagePermissionGranted(context)) {
            NotificationHelper.showNotification(
                context,
                1002,
                "Usage Access Needed",
                "Tap to allow usage access",
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                NotificationHelper.IntentType.ACTIVITY
            )
        }
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

    private fun Context.getPrefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
