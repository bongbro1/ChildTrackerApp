package com.example.childtrackerapp.child.data

import android.location.Location
import com.example.childtrackerapp.model.VoiceMessage
import com.google.firebase.database.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class ChildRepository( val childId: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private var cachedName: String? = null

    suspend fun getChildName(): String {
        if (cachedName == null) {
            cachedName = loadChildName()
        }
        return cachedName!!
    }

    suspend fun loadChildName(): String {
        val snapshot = db.child("users").child(childId).child("name").get().await()
        return snapshot.getValue(String::class.java) ?: "Unknown"
    }

    //Tin nhắn từ cha
    private val _voiceMessageFromParent = MutableStateFlow<String?>(null)
    val voiceMessageFromParent: StateFlow<String?> = _voiceMessageFromParent

    // Lắng nghe tin nhắn từ cha
    fun startListeningFromParent() {
        db.child("messages").child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(VoiceMessage::class.java)
                msg?.let {
                    if (it.from == "parent") {
                        _voiceMessageFromParent.value = it.text
                        db.child("messages").child(childId).removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Con gửi tin nhắn → Cha
    suspend fun sendVoiceMessageToParent(message: String) {
        val msg = VoiceMessage(
            id = childId,
            text = message,
            timestamp = System.currentTimeMillis(),
            from = "child",
            to = "parent1"
        )

        db.child("messages").child(childId).setValue(msg).await()
        // Xóa sau 10 giây
//        kotlinx.coroutines.delay(10_000L)
//        db.child("messages").child(childId).removeValue()
    }
    suspend fun sendLocation(location: Location) {
        val name =getChildName()
        val data = mapOf(
            "childId" to childId,
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "name" to name
        )

        db.child("locations").child(childId).setValue(data).await()
    }


}

