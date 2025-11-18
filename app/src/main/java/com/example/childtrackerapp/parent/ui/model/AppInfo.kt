package com.example.childtrackerapp.parent.ui.model

import android.graphics.Bitmap

data class AppInfo(
    val name: String = "",
    val packageName: String = "",
    val iconBase64: String? = null,
    val isAllowed: Boolean = false,
    val usageTime: String = ""
)