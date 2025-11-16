package com.example.childtrackerapp.parent.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.childtrackerapp.parent.data.ParentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ParentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ParentRepository()
    val childLocations = repository.childLocations

    private val _voiceSendResult = MutableStateFlow<Result<Boolean>?>(null)
    val voiceSendResult = _voiceSendResult


    init {
        loadChildrenLocations()


    }
    fun loadChildrenLocations() {
        val parentId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.listenChildrenLocations(parentId)
    }
    fun sendVoiceFile(childId: String, file: File) {
        viewModelScope.launch {
            try {
                // 🔹 Kiểm tra file trước
                if (!file.exists()) {
                    Log.e("VoiceUpload", "❌ File không tồn tại: ${file.absolutePath}")
                    _voiceSendResult.value = Result.failure(
                        Exception("File không tồn tại")
                    )
                    return@launch
                }

                if (file.length() == 0L) {
                    Log.e("VoiceUpload", "❌ File rỗng (0 bytes)")
                    _voiceSendResult.value = Result.failure(
                        Exception("File rỗng")
                    )
                    return@launch
                }

                Log.d("VoiceUpload", "📤 Bắt đầu upload...")
                Log.d("VoiceUpload", "   File: ${file.name}")
                Log.d("VoiceUpload", "   Kích thước: ${file.length()} bytes")
                Log.d("VoiceUpload", "   Đường dẫn: ${file.absolutePath}")

                // 🔹 Tạo đường dẫn Firebase Storage
                val timestamp = System.currentTimeMillis()
                val fileName = "voice_${timestamp}.wav"
                val storagePath = "voices/$childId/$fileName"

                // 🔹 Upload lên Firebase
                val storageRef = FirebaseStorage.getInstance().reference
                val fileRef = storageRef.child(storagePath)

                fileRef.putFile(Uri.fromFile(file)).await()

                Log.d("VoiceUpload", "✅ Upload thành công: $storagePath")

                // 🔹 Xóa file cục bộ sau khi upload xong
                file.delete()

                _voiceSendResult.value = Result.success(true)

            } catch (e: Exception) {
                Log.e("VoiceUpload", "❌ Lỗi upload: ${e.message}", e)
                e.printStackTrace()
                _voiceSendResult.value = Result.failure(e)
            }
        }
    }


    fun listenChild(childId: String) {
        repository.startListeningFromChild(childId)
    }

    override fun onCleared() {
        super.onCleared()

    }

    fun resetVoiceSendResult() {
        _voiceSendResult.value = null
    }

}


