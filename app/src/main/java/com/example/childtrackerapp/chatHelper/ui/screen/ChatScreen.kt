package com.example.childtrackerapp.chatHelper.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.childtrackerapp.chatHelper.data.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatScreen(
    childId: String,
    childName: String,
    viewModel: ChatViewModel = viewModel(),
    currentUserId: String = FirebaseAuth.getInstance().currentUser!!.uid
) {


    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 🔥 Auto scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    // 🔥 Load và lắng nghe tin nhắn realtime
    LaunchedEffect(childId) {
        viewModel.subscribeRealtimeMessages(childId) // hoặc loadMessages nếu không realtime
    }


    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        Text(
            text = "💬 Chat với $childName",
            fontSize = 20.sp
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                ChatBubble(msg, currentUserId)
                Spacer(Modifier.height(4.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") }
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(childId, text, currentUserId)
                        text = ""
                    }
                }
            ) {
                Text("Gửi")
            }
        }
    }
}

