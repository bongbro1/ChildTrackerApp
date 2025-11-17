package com.example.childtrackerapp.Athu.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.child.ui.MainActivity_Child
import com.example.childtrackerapp.databinding.ActivityLoginBinding
import com.example.childtrackerapp.parent.ui.view.ParentMainActivity

import kotlinx.coroutines.flow.collectLatest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // 🔹 Kiểm tra user đã login chưa
        if (sessionManager.isLoggedIn()) {
            val role = sessionManager.getRole()
            navigateByRole(role ?: "")
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.authState.collectLatest { result ->
                result?.onSuccess { user ->
                    // ✅ Lưu session khi đăng nhập thành công
                    sessionManager.saveSession(
                        userId = user.uid,
                        email = user.email,
                        role = user.role,
                        userName = user.name
                    )

                    Toast.makeText(this@LoginActivity, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    navigateByRole(user.role)
                }?.onFailure {
                    Toast.makeText(this@LoginActivity, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearAuthState()
            }
        }
    }

    private fun navigateByRole(role: String) {
        when (role) {
            "cha" -> startActivity(Intent(this, ParentMainActivity::class.java))
            "con" -> startActivity(Intent(this, MainActivity_Child::class.java))
        }
        finish()
    }
}