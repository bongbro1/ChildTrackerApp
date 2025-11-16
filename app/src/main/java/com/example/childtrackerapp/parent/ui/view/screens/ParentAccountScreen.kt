package com.example.childtrackerapp.parent.ui.view.screens



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel

import com.example.childtrackerapp.parent.ui.components.ChildRow
import kotlinx.coroutines.launch

@Composable
fun ParentAccountScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Khi parent login → load children
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentUser!!.role == "cha") {
            viewModel.loadChildrenForParent(currentUser!!.uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Quản lý tài khoản con", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        // --- FORM TẠO TÀI KHOẢN CON ---
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên con") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email con") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu con") },
            modifier = Modifier.fillMaxWidth()
        )

        val coroutineScope = rememberCoroutineScope()

        Button(
            onClick = {
                coroutineScope.launch {
                    val result = viewModel.createChildAccountSafe(name, email, password)
                    result.onSuccess {
                        message = "Tạo tài khoản con thành công"
                        name = ""; email = ""; password = ""
                        viewModel.loadChildrenForParent(currentUser!!.uid)
                    }.onFailure {
                        message = it.message ?: "Lỗi không xác định"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tạo tài khoản con")
        }

        if (message.isNotEmpty()) {
            Text(text = message, color = Color.Green)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Danh sách tài khoản con",
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // --- LIST CON ---
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(children) { child ->
                ChildRow(
                    user = child,
                    onChatClick = {
                        navController.navigate("chat/${child.uid}/${child.name}")
                    },
                    onClick = {
                        // profile con nếu bạn muốn mở sau
                    }
                )
            }
        }
    }
}



