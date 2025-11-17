package com.example.childtrackerapp.child.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.child.viewmodel.ChildViewModel

@Composable

fun ChildLocationScreen(
    childModel: ChildViewModel,
    onOpenChat: () -> Unit
) {
    Column {
        Text("Vị trí của con...")

        Button(onClick = onOpenChat) {
            Text("Nhắn tin với cha")
        }
    }
}

@Composable
fun ListScreen(modifier: Modifier = Modifier) {
    Text("Danh sách", modifier = modifier)
}

@Composable fun AccountScreen(modifier: Modifier = Modifier,authVM: AuthViewModel) {
    Text("Tài khoản", modifier = modifier) }
@Composable fun ScheduleScreen(modifier: Modifier = Modifier) {
    Text("Lịch trình", modifier = modifier)
}
@Composable fun MenuScreen(modifier: Modifier = Modifier) {
    Text("Menu", modifier = modifier)
}
