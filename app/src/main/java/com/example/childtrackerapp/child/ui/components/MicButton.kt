package com.example.childtrackerapp.child.ui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.childtrackerapp.R
import com.example.childtrackerapp.parent.ui.components.startListening

@Composable
fun MicButton(onText: (String) -> Unit) {
    val ctx = LocalContext.current
    val recognizer = remember { android.speech.SpeechRecognizer.createSpeechRecognizer(ctx) }
    var isListening by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = {
            if (!isListening) {
                startListening(ctx, recognizer) { text -> onText(text) }
            } else recognizer.stopListening()

            isListening = !isListening
        }
    ) {
        Icon(
            painter = painterResource(
                if (isListening) R.drawable.ic_mic_on else R.drawable.ic_mic
            ),
            contentDescription = "Mic"
        )
    }
}
