package com.example.childtrackerapp.parent.ui.components.cpn_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.childtrackerapp.parent.helper.toImageBitmapOrNull
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.example.childtrackerapp.utils.BackgroundSecondary
import com.example.childtrackerapp.utils.Primary
import java.time.LocalTime


@Composable
fun AppItem(
    app: AppInfo,
    onSaveSettings: (isAllowed: Boolean, startTime: LocalTime, endTime: LocalTime, allowedDays: List<String>) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBitmap = app.iconBase64.toImageBitmapOrNull()
            iconBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.name,
                    modifier = Modifier.size(48.dp).padding(end = 12.dp)
                )
            } ?: Box(modifier = Modifier.size(48.dp).padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleMedium)
                Text(app.usageTime ?: "0m", style = MaterialTheme.typography.bodySmall)
            }

            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = "Edit app settings",
                tint = Primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showSettings = true
                    }
            )
        }
    }

    if (showSettings) {
        var isAllowed by remember { mutableStateOf(app.allowed) }
        var startTime by remember { mutableStateOf(LocalTime.parse(app.startTime.ifBlank { "08:00" })) }
        var endTime by remember { mutableStateOf(LocalTime.parse(app.endTime.ifBlank { "20:00" })) }

        val handleSave: (Boolean, LocalTime, LocalTime, List<String>) -> Unit = { newIsAllowed, newStartTime, newEndTime, newDays ->
            isAllowed = newIsAllowed
            startTime = newStartTime
            endTime = newEndTime
            showSettings = false
            onSaveSettings(newIsAllowed, newStartTime, newEndTime, newDays)
        }

        AppSettingsDialog(
            appName = app.name,
            initialIsAllowed = isAllowed,
            initialStartTime = startTime,
            initialEndTime = endTime,
            initialAllowedDays = app.allowedDays,
            onDismiss = { showSettings = false },
            onSave = handleSave
        )
    }
}