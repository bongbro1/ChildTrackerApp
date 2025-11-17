package com.example.childtrackerapp.child.helper

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAlertHelper(context: Context) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN")
            }
        }
    }

    fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun release() {
        tts?.shutdown()
        tts = null
    }
}