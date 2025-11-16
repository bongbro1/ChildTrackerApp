package com.example.childtrackerapp.chatHelper.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.model.ChatMessage

@Composable
fun ChatBubble(msg: ChatMessage, currentUser: String) {
    val isMe = msg.sender == currentUser
    val timeText = remember(msg.timestamp) {
        val date = java.util.Date(msg.timestamp)
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isMe) Color(0xFFCEECFF) else Color(0xFFECECEC),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Text(msg.text)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}



