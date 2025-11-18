package com.example.childtrackerapp.parent.ui.view

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.model.User
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsViewModel

import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel
import com.example.childtrackerapp.ui.theme.ChildTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
@AndroidEntryPoint
class ParentMainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val allowedAppsViewModel: AllowedAppsViewModel by viewModels()
    private val parentViewModel: ParentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                try {
                    val snapshot = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(firebaseUser.uid)
                        .get()
                        .await()
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        authViewModel.setCurrentUser(user)
                    } else {
                        Toast.makeText(
                            this@ParentMainActivity,
                            "Không tìm thấy thông tin tài khoản cha",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ParentMainActivity,
                        "Lỗi khi tải user: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this@ParentMainActivity, "Chưa đăng nhập", Toast.LENGTH_SHORT).show()
            }
        }


//        if (!checkUsagePermission()) {
//            requestUsagePermission()
//            Toast.makeText(
//                this,
//                "Vui lòng cấp quyền Truy cập dữ liệu ứng dụng để xem thời gian sử dụng",
//                Toast.LENGTH_LONG
//            ).show()
//        }

        setContent {
            ChildTrackerTheme {
                ParentMainScreen(authViewModel = authViewModel,
                    parentViewModel = parentViewModel,
                    allowedAppsViewModel = allowedAppsViewModel)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Kiểm tra lại khi quay lại Activity từ Cài đặt
        if (checkUsagePermission()) {
            // Quyền đã được cấp, có thể load lại dữ liệu hoặc refresh UI
            Toast.makeText(
                this,
                "OK",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Quyền chưa có, bạn có thể nhắc nhở người dùng
            Toast.makeText(
                this,
                "Vui lòng cấp quyền Truy cập dữ liệu ứng dụng để xem thời gian sử dụng",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun checkUsagePermission(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsagePermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}
