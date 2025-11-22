package com.example.childtrackerapp.parent.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.admin.MainActivity
import com.example.childtrackerapp.helpers.NotificationHelper
import com.example.childtrackerapp.parent.ui.model.ViolationLog
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val db = FirebaseDatabase.getInstance().reference
    private val _logs = MutableLiveData<List<ViolationLog>>()
    private val logList = mutableListOf<ViolationLog>()

    fun startListeningLogs() {
        val parentId = sessionManager.getUserId() ?: return
        fetchChildIds(parentId) { childIds ->
            childIds.forEach { childId ->
                listenChildLogs(childId)
            }
        }
    }

    fun testFakeLog() {
        // 1. Tạo log giả lập
        val fakeLog = ViolationLog(
            packageName = "com.example.childtrackerapp", // app giả lập
            name = "ChildTrackerApp",
            violatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        )

        // 2. Thêm vào danh sách log
        logList.add(0, fakeLog) // log mới lên đầu
        _logs.postValue(logList.toList()) // cập nhật LiveData

        // 3. Hiển thị notification
        val intent = Intent(context, MainActivity::class.java)

        NotificationHelper.showNotification(
            context = context,
            id = fakeLog.hashCode(),
            title = "Thông báo giả lập",
            text = "Hiện Test Child đang mở app ${fakeLog.name} ngoài khoảng thời gian cho phép",
            intent = intent
        )
    }

    // 1. Fetch danh sách childId của parent
    private fun fetchChildIds(parentId: String, callback: (List<String>) -> Unit) {
        db.child("users").orderByChild("parentId").equalTo(parentId)
            .get().addOnSuccessListener { snapshot ->
                val childIds = snapshot.children.mapNotNull { it.key }
                callback(childIds)
            }.addOnFailureListener {
                callback(emptyList())
            }
    }

    // 2. Lắng nghe notification_logs cho từng child
    private fun listenChildLogs(childId: String) {
        db.child("blocked_items/$childId/notification_logs")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, prevKey: String?) {
                    val log = snapshot.getValue(ViolationLog::class.java) ?: return
                    // Thêm vào list
                    logList.add(0, log)
                    _logs.postValue(logList.toList())
                    // Nếu log chưa được thông báo, push notification
                    if (!log.notified) {
                        // Lấy tên child
                        db.child("users/$childId/name").get()
                            .addOnSuccessListener { nameSnapshot ->
                                val childName = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                val intent = Intent(context, MainActivity::class.java)

                                NotificationHelper.showNotification(
                                    context = context,
                                    id = log.hashCode(),
                                    title = "Thông báo",
                                    text = "$childName đang mở app ${log.name}.",
                                    intent = intent
                                )
                                // Cập nhật flagged notified = true
                                snapshot.ref.child("notified").setValue(true)
                            }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, prevKey: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, prevKey: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

}
