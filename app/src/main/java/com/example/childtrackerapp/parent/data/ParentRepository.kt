package com.example.childtrackerapp.parent.data

import android.net.Uri
import com.example.childtrackerapp.model.ChildLocation
import com.example.childtrackerapp.model.User
import com.example.childtrackerapp.model.VoiceMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class ParentRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // 🔹 Lưu vị trí của con
    private val _childLocations = MutableStateFlow<Map<String, ChildLocation>>(emptyMap())
    val childLocations: StateFlow<Map<String, ChildLocation>> = _childLocations

    // 🔹 Lưu tin nhắn từ con
    private val _voiceMessageFromChild = MutableStateFlow<String?>(null)
    val voiceMessageFromChild: StateFlow<String?> = _voiceMessageFromChild

    fun listenChildrenLocations(parentId: String) {
        // Lấy user theo parentId
        db.child("users").orderByChild("parentId").equalTo(parentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val childIds = snapshot.children.mapNotNull { it.key }

                    if (childIds.isEmpty()) {
                        _childLocations.value = emptyMap()
                        return
                    }

                    // Lắng nghe vị trí của từng con
                    childIds.forEach { childId ->
                        db.child("locations").child(childId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(locSnap: DataSnapshot) {
                                    val lat = locSnap.child("lat").getValue(Double::class.java)
                                    val lng = locSnap.child("lng").getValue(Double::class.java)
                                    val name = locSnap.child("name").getValue(String::class.java)

                                    if (lat != null && lng != null) {
                                        val newMap = _childLocations.value.toMutableMap()
                                        newMap[childId] = ChildLocation(lat, lng,name)
                                        _childLocations.value = newMap
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }



    // 🔹 Lắng nghe tin nhắn từ con
    fun startListeningFromChild(childId: String) {
        db.child("messages").child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(VoiceMessage::class.java)
                msg?.let {
                    if (it.from == "child") {
                        _voiceMessageFromChild.value = it.text
                        db.child("messages").child(childId).removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun createChildAccount(
        name: String,
        email: String,
        password: String,
        parentId: String
    ): User {
        val uid = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .await().user?.uid ?: throw Exception("Cannot create child")

        val child = User(
            uid = uid,
            name = name,
            email = email,
            role = "child",
            parentId = parentId
        )

        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .setValue(child)

        return child
    }

    suspend fun sendVoiceMessage(childId: String, audioFile: File): String {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("voiceMessages/$childId/${audioFile.name}")

        val url = storageRef.putFile(Uri.fromFile(audioFile))
            .await()
            .storage.downloadUrl.await()
            .toString()

        val messageData = mapOf(
            "audioUrl" to url,
            "timestamp" to System.currentTimeMillis(),
            "from" to "parent"
        )

        FirebaseDatabase.getInstance().reference
            .child("messages")
            .child(childId)
            .push()
            .setValue(messageData)

        return url  // 🔥 MUST RETURN
    }




}
