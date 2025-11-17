package com.example.childtrackerapp.schedule

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class - Khởi tạo Firebase và Hilt
 */
@HiltAndroidApp
class ScheduleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)
    }
}
