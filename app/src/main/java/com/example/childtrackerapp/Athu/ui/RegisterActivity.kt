package com.example.childtrackerapp.Athu.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel

import com.example.childtrackerapp.child.ui.MainActivity_Child
import com.example.childtrackerapp.databinding.ActivityRegisterBinding
import com.example.childtrackerapp.parent.ui.view.ParentMainActivity

import kotlinx.coroutines.flow.collectLatest

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val selectedId = binding.radioGroupRole.checkedRadioButtonId
            val role = findViewById<RadioButton>(selectedId)?.text.toString().lowercase()

            if (email.isEmpty() || password.isEmpty() || role.isEmpty()) {

                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.tvError.setText("Mật khẩu phải có it nhất 6 kí tự")
                return@setOnClickListener
            }

            viewModel.register(email, password, role)
        }


        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.authState.collectLatest { result ->
                result?.onSuccess { user ->
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                    navigateByRole(user.role)
                }?.onFailure {
                    Toast.makeText(this@RegisterActivity, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Đăng ký thất bại", it)

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