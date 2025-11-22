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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.child.helper.PermissionHelper
import com.example.childtrackerapp.child.ui.screen.ChildMainScreen
import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.helpers.NotificationHelper
import com.example.childtrackerapp.helpers.WorkerScheduler
import com.example.childtrackerapp.service.AppMonitorService
import com.example.childtrackerapp.service.LocationService
import com.example.childtrackerapp.service.NotificationPermissionActivity
import com.example.childtrackerapp.ui.theme.ChildTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import kotlin.getValue


@AndroidEntryPoint
class MainActivity_Child : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var workerScheduler: WorkerScheduler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startLocationServiceIfPermitted()

        PermissionHelper.showUsageAccessNotificationIfNeeded(this) // quyền truy cập time sử dụng các app

        workerScheduler.scheduleSendAppsWorker(this) // len lich gửi danh sách các app lên firebase
        workerScheduler.scheduleAppStatusWorker(this) // len lich check app

        // Check và request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(this, NotificationPermissionActivity::class.java))
            }
        }

        // service giám sát app
        val intent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        setContent {
            ChildTrackerTheme {
                val childViewModel: ChildViewModel = hiltViewModel()
                ChildMainScreen(authViewModel, childViewModel)
            }
        }
    }

    private fun startLocationServiceIfPermitted() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fg = if (Build.VERSION.SDK_INT >= 34)
            checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        else
            PackageManager.PERMISSION_GRANTED

        if (fine == PackageManager.PERMISSION_GRANTED &&
            coarse == PackageManager.PERMISSION_GRANTED &&
            fg == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, LocationService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
                ), 1001
            )
        }
    }



}
