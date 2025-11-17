package com.example.childtrackerapp.chatHelper.data

import com.example.childtrackerapp.model.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatRepository {

    private val db = FirebaseDatabase.getInstance().getReference("chats")

    fun getMessagesFlow(childId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child(childId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendMessage(childId: String, msg: ChatMessage) {
        db.child(childId).push().setValue(msg)
    }
}
