package com.example.childtrackerapp.child.helper

import android.widget.Toast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.childtrackerapp.model.ChildLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceSendBottomSheet(
    children: Map<String, ChildLocation>,
    onSend: (childId: String, file: File) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val recorder = remember { AudioRecorderHelper(ctx) }

    var selectedChild by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }

    val permissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (!permissionState.status.isGranted) {
        Column(Modifier.padding(16.dp)) {
            Text("⚠️ Bạn cần cấp quyền micro để ghi âm")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Cấp quyền")
            }
        }
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {

        Text("Chọn con để gửi ghi âm", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        children.forEach { (childId, child) ->
            ListItem(
                headlineContent = { Text(child.name ?: "Không tên") },
                supportingContent = { Text("ID: $childId") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedChild = childId }
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(15.dp))

        // ⭐ NÚT START / STOP
        Button(
            onClick = {
                if (selectedChild == null) {
                    Toast.makeText(ctx, "⚠️ Hãy chọn con trước khi ghi âm", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!isRecording) {
                    // 👉 START RECORDING
                    isRecording = true
                    recordedFile = recorder.startRecording()
                    Toast.makeText(ctx, "🎙 Bắt đầu ghi âm...", Toast.LENGTH_SHORT).show()

                } else {
                    // 👉 STOP RECORDING
                    val file = recorder.stopRecording()
                    isRecording = false

                    if (file != null && file.length() > 0) {
                        Toast.makeText(ctx, "✔ Ghi âm hoàn tất", Toast.LENGTH_SHORT).show()
                        onSend(selectedChild!!, file)
                        onDismiss()
                    } else {
                        Toast.makeText(ctx, "⚠ Không có dữ liệu ghi âm", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isRecording) "⏹ Dừng ghi" else "⏺ Bắt đầu ghi",
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔴 Đang ghi âm...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
