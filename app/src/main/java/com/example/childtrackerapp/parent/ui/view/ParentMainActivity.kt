package com.example.childtrackerapp.parent.ui.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.model.User

import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel
import com.example.childtrackerapp.ui.theme.ChildTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ParentMainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
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

        setContent {
            ChildTrackerTheme {
                ParentMainScreen(authViewModel = authViewModel,
                    parentViewModel = parentViewModel)
            }
        }
    }
}
