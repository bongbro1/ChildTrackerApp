package com.example.childtrackerapp.parent.ui.view.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.childtrackerapp.Athu.ui.LoginActivity
import com.example.childtrackerapp.Athu.viewmodel.Logoutable
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel

@Composable
fun LogOutScreen(
    viewModel: Logoutable
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logout()   // Xóa session

        // Chuyển sang LoginActivity
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        context.startActivity(intent)
    }
}

