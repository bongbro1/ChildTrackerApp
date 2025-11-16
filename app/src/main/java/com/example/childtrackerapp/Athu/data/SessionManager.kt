package com.example.childtrackerapp.Athu.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )
    private val editor = sharedPref.edit()

    // 🔹 Lưu session khi đăng nhập
    fun saveSession(userId: String, email: String, role: String, userName: String) {
        editor.apply {
            putString("user_id", userId)
            putString("email", email)
            putString("role", role)
            putString("user_name", userName)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    // 🔹 Lấy thông tin user đã lưu
    fun getUserId(): String? = sharedPref.getString("user_id", null)
    fun getEmail(): String? = sharedPref.getString("email", null)
    fun getRole(): String? = sharedPref.getString("role", null)
    fun getUserName(): String? = sharedPref.getString("user_name", null)

    // 🔹 Kiểm tra user đã login chưa
    fun isLoggedIn(): Boolean = sharedPref.getBoolean("is_logged_in", false)

    // 🔹 Xóa session (logout)
    fun clearSession() {
        editor.apply {
            remove("user_id")
            remove("email")
            remove("role")
            remove("user_name")
            putBoolean("is_logged_in", false)
            apply()
        }
    }
}