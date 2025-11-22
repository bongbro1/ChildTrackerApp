package com.example.childtrackerapp.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.childtrackerapp.R
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "perm_channel"
    private const val CHANNEL_NAME = "Permissions"

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getNotificationManager(context)
            val existingChannel = nm.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildPendingIntent(context: Context, id: Int, intent: Intent, type: IntentType): PendingIntent {
        return when(type) {
            IntentType.ACTIVITY -> PendingIntent.getActivity(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            IntentType.SERVICE -> PendingIntent.getService(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            IntentType.BROADCAST -> PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        intent: Intent,
        type: IntentType = IntentType.ACTIVITY // Mặc định là Activity
    ) {
        createChannelIfNeeded(context)
        val nm = getNotificationManager(context)

        val pendingIntent = buildPendingIntent(context, id, intent, type)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(id, notification)
    }

    enum class IntentType {
        ACTIVITY,
        SERVICE,
        BROADCAST
    }
}
