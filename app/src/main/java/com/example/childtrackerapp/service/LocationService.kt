package com.example.childtrackerapp.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.childtrackerapp.R
import com.example.childtrackerapp.child.data.ChildRepository
import com.example.childtrackerapp.child.helper.GeoFenceHelper
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: ChildRepository

    override fun onCreate() {
        super.onCreate()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return stopSelf()
        repository = ChildRepository(uid)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        startForegroundServiceNotification()
        checkIfGpsEnabled()
        requestLocationUpdates()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Child Tracking", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Đang theo dõi vị trí")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()

        startForeground(1, notif)
    }

    private fun checkIfGpsEnabled() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val i = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
        }
    }

    private fun requestLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        fusedLocationClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            CoroutineScope(Dispatchers.IO).launch {
                repository.sendLocation(loc)
            }
            GeoFenceHelper.checkDangerZone(this@LocationService, loc)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
