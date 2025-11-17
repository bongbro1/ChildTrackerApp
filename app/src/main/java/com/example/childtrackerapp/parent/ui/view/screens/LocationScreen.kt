package com.example.childtrackerapp.parent.ui.view

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.childtrackerapp.child.helper.VoiceSendBottomSheet
import com.example.childtrackerapp.parent.ui.components.OSMMapView
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(viewModel: ParentViewModel) {

    val locations by viewModel.childLocations.collectAsState()
    val voiceSendResult by viewModel.voiceSendResult.collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutine = rememberCoroutineScope()

    // Scaffold to show snackbar
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        // Show bottom sheet
        if (showSheet) {
            VoiceSendBottomSheet(
                children = locations,
                onSend = { childId, file ->
                    viewModel.sendVoiceFile(childId, file)
                    showSheet = false
                },
                onDismiss = { showSheet = false }
            )
        }




        // Listen for upload result
        LaunchedEffect(voiceSendResult) {
            voiceSendResult?.let { result ->
                if (result.isSuccess) {
                    coroutine.launch { snackbarHostState.showSnackbar("Gửi thành công!") }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("VoiceMessageUI", "Send failed: ${error?.message}", error)

                    coroutine.launch {
                        snackbarHostState.showSnackbar(
                            "Tính Năng Đang cải tiến"
                        )
                    }
                }

                // 🔹 Reset tránh trigger lại khi recomposition
                viewModel.resetVoiceSendResult()
            }
        }


        // Main UI (Map)
        OSMMapView(
            modifier = Modifier.fillMaxSize(),
            locations = locations,
            onMicClick = { showSheet = true }
        )
    }
}
