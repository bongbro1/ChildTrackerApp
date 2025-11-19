package com.example.childtrackerapp.Athu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.Athu.data.AuthRepository
import com.example.childtrackerapp.model.ChatMessage
import com.example.childtrackerapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    // Trạng thái auth chung (login/register)
    private val _authState = MutableStateFlow<Result<User>?>(null)
    val authState: StateFlow<Result<User>?> = _authState

    // Danh sách con
    private val _children = MutableStateFlow<List<User>>(emptyList())
    val children: StateFlow<List<User>> get() = _children

    // User hiện tại (cha)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> get() = _currentUser

    // Trạng thái tạo child riêng biệt
    private val _createChildState = MutableStateFlow<Result<String>?>(null)
    val createChildState: StateFlow<Result<String>?> = _createChildState

    // ---------------------- LOGIN / REGISTER ----------------------
    fun register(email: String, password: String, role: String) {
        viewModelScope.launch {
            val result = repo.registerUser(email, password, role)
            _authState.value = result
            result.onSuccess { user ->
                _currentUser.value = user
                refreshCurrentUser() // load children nếu là cha
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repo.loginUser(email, password)
            _authState.value = result
            result.onSuccess { user ->
                _currentUser.value = user
                refreshCurrentUser() // load children nếu là cha
            }
        }
    }

    // ---------------------- REFRESH CHILDREN ----------------------
    fun refreshCurrentUser() {
        val user = _currentUser.value
        if (user != null && user.role == "cha") {
            loadChildrenForParent(user.uid)
        }
    }

    fun loadChildrenForParent(parentId: String) {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .orderByChild("parentId")
                    .equalTo(parentId)
                    .get()
                    .await()

                val list = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                Log.d("AuthViewModel", "Loaded ${list.size} children from Firebase")
                list.forEach { Log.d("AuthViewModel", "Child: ${it.name}, ${it.email}") }
                _children.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---------------------- CREATE CHILD ----------------------
    suspend fun createChildAccountSafe(
        name: String,
        email: String,
        password: String
    ): Result<User> {
        val parentId = _currentUser.value?.uid
            ?: return Result.failure(Exception("Không xác định được tài khoản cha"))

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập đầy đủ thông tin"))
        }

        return try {
            val child = repo.createChildAccount(name, email, password, parentId)

            // Cập nhật danh sách children tự động
            val updatedList = _children.value.toMutableList()
            updatedList.add(child)
            _children.value = updatedList

            Result.success(child)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------- HELPERS ----------------------
    fun clearAuthState() {
        _authState.value = null
    }

    fun setChildren(list: List<User>) {
        _children.value = list
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
        refreshCurrentUser() // load children nếu là cha
    }

    fun loadUserFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val snapshot = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .get()
                    .await()

                val user = snapshot.getValue(User::class.java)
                _currentUser.value = user

                Log.d("AuthViewModel", "User loaded: $user")

                refreshCurrentUser()

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi load user: ${e.message}")
            }
        }
    }



}

interface Logoutable {
    suspend fun logout()
}