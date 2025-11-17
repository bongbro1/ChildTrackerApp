package com.example.childtrackerapp.Athu.data


import com.example.childtrackerapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    suspend fun registerUser(email: String, password: String, role: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID null"))
            val user = User(uid, email, role)
            db.child("users").child(uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID null"))

            val snapshot = db.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("Không tìm thấy người dùng"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChildAccount(
        name: String,
        email: String,
        password: String,
        parentId: String
    ): User {
        // 1️⃣ Tạo tài khoản con mà không làm đăng xuất tài khoản cha
        val tempAuth = FirebaseAuth.getInstance()
        val result = tempAuth.createUserWithEmailAndPassword(email, password).await()
        val childId = result.user?.uid ?: throw Exception("Không tạo được tài khoản con")

        // 2️⃣ Tạo object user con
        val child = User(
            uid = childId,
            name = name,
            email = email,
            role = "con",
            parentId = parentId
        )

        // 3️⃣ Ghi dữ liệu vào /users/childId
        db.child("users").child(childId).setValue(child).await()

        // 4️⃣ Đăng xuất tài khoản con → giữ nguyên cha đang đăng nhập
        tempAuth.signOut()

        return child
    }

}
