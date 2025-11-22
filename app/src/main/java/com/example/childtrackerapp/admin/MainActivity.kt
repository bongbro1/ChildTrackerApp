package com.example.childtrackerapp.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.navigation.AppNavHost
import com.example.childtrackerapp.schedule.ui.theme.ChildTrackerAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var repository: ScheduleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo dữ liệu mẫu nếu chưa có (chỉ chạy lần đầu)
        lifecycleScope.launch {
            try {
                repository.initializeSampleData()
            } catch (e: Exception) {
                // Log error nhưng không crash app
                e.printStackTrace()
            }
        }
        setContent {
            ChildTrackerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }

    }

}