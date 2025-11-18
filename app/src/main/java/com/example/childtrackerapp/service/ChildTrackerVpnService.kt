package com.example.childtrackerapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.os.Build

class ChildTrackerVpnService : VpnService() {

    companion object {
        private const val TAG = "ChildTrackerVpnService"
        var instance: ChildTrackerVpnService? = null
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "onCreate called")
        startForegroundService()
        prepareAndStartVpn()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
    fun setBlockedWebsites(websites: List<String>) {
        Log.d(TAG, "Blocked domains set: $websites")
        // nếu bạn có DNS proxy, cập nhật danh sách ở đây
    }

    private fun startForegroundService() {
        val channelId = "vpn_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Child Tracker VPN")
            .setContentText("VPN is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 12+ yêu cầu FLAG, nhưng bạn có thể dùng FLAG_NONE
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(1, notification)
        }
    }


    inner class LocalBinder : Binder() {
        fun getService(): ChildTrackerVpnService = this@ChildTrackerVpnService
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            Log.d(TAG, "User consent required for VPN")
            // Bạn cần startActivityForResult từ Activity để xin quyền
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val builder = Builder()

        // VPN local IP
        builder.addAddress("10.0.0.2", 32)

        // Route tất cả traffic ra Internet
        builder.addRoute("0.0.0.0", 0)
        builder.addRoute("::", 0) // nếu muốn hỗ trợ IPv6

        // DNS hợp lệ
        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("8.8.4.4")

        // Tên VPN
        builder.setSession("ChildTrackerVPN")

        vpnInterface = builder.establish()
        Log.d(TAG, "VPN started successfully")
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        Log.d(TAG, "VPN stopped")
    }
}
