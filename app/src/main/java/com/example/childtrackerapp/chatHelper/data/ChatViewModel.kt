package com.example.childtrackerapp.chatHelper.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.model.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun loadMessages(childId: String) {
        viewModelScope.launch {
            chatRepo.getMessagesFlow(childId).collect {
                _messages.value = it.sortedBy { m -> m.timestamp }
            }
        }
    }

    fun subscribeRealtimeMessages(childId: String) {
        val ref = database.getReference("messages/$childId")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                _messages.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    var lastMessageCount = 0

    fun subscribeRealtimeMessagesWithNotification(
        childId: String,
        onNewMessage: (ChatMessage) -> Unit
    ) {
        val ref = database.getReference("messages/$childId")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }

                if (list.size > lastMessageCount && lastMessageCount != 0) {
                    onNewMessage(list.last())
                }

                lastMessageCount = list.size
                _messages.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendMessage(childId: String, text: String, parentId: String) {
        if (text.isBlank()) return

        val ref = database.getReference("messages/$childId")
        val msgId = ref.push().key ?: return

        val msg = ChatMessage(
            id = msgId,
            sender = parentId,
            receiver = childId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        ref.child(msgId).setValue(msg)
    }
}

