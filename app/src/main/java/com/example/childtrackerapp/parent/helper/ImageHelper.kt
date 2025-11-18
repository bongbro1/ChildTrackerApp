package com.example.childtrackerapp.parent.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun String?.toImageBitmapOrNull(): ImageBitmap? {
    if (this.isNullOrEmpty()) return null
    return try {
        val bytes = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper extension function
fun String.toBitmapFromBase64(): Bitmap? {
    return try {
        val bytes = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}