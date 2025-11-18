package com.example.childtrackerapp.Athu.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPref: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )
    private val editor = sharedPref.edit()

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

    fun getUserId(): String? = sharedPref.getString("user_id", null)
    fun getEmail(): String? = sharedPref.getString("email", null)
    fun getRole(): String? = sharedPref.getString("role", null)
    fun getUserName(): String? = sharedPref.getString("user_name", null)

    fun isLoggedIn(): Boolean = sharedPref.getBoolean("is_logged_in", false)

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

    fun logout() = clearSession()
}
