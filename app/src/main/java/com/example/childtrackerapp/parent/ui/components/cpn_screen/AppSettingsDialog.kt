package com.example.childtrackerapp.parent.ui.components.cpn_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.utils.Primary
import com.example.childtrackerapp.utils.WEEK_DAYS
import com.example.childtrackerapp.utils.autoFormatTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSettingsDialog(
    appName: String,
    initialIsAllowed: Boolean,
    initialStartTime: LocalTime,
    initialEndTime: LocalTime,
    initialAllowedDays: List<String> = WEEK_DAYS,
    onDismiss: () -> Unit,
    onSave: (isAllowed: Boolean, startTime: LocalTime, endTime: LocalTime, allowedDays: List<String>) -> Unit
) {
    var isAllowed by remember { mutableStateOf(initialIsAllowed) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    val allowedDays = remember { mutableStateListOf<String>().apply { addAll(initialAllowedDays) }}
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var startInput by remember { mutableStateOf(TextFieldValue(initialStartTime.format(formatter))) }
    var endInput by remember { mutableStateOf(TextFieldValue(initialEndTime.format(formatter))) }

    AlertDialog(
        shape = RoundedCornerShape(12.dp),
        onDismissRequest = onDismiss,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cài đặt thời gian sử dụng\n$appName",
                    fontSize = 20.sp,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = Center
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center){
                Column(modifier = Modifier.fillMaxWidth().padding(start = 3.dp)) {

                    val labelWidth = 140.dp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Cho phép sử dụng: ",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(135.dp)
                        )
                        Switch(
                            checked = isAllowed,
                            onCheckedChange = { isAllowed = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeInputField(
                            label = "Thời gian bắt đầu: ",
                            value = startInput,
                            onValueChange = { startInput = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeInputField(
                            label = "Thời gian kết thúc: ",
                            value = endInput,
                            onValueChange = { endInput = it },
                            isEndTime = true,
                            startTime = startInput
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Ngày được phép: ", fontWeight = FontWeight.Medium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WEEK_DAYS.forEach { day ->
                            val selected = allowedDays.contains(day)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) allowedDays.remove(day)
                                    else allowedDays.add(day)
                                },
                                label = { Text(day, textAlign = Center, modifier = Modifier.fillMaxWidth(),) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Primary
                                ),
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val st = try { LocalTime.parse(startInput.text) } catch (e: Exception) { LocalTime.of(8,0) }
                    val et = try { LocalTime.parse(endInput.text) } catch (e: Exception) { LocalTime.of(20,0) }

                    // đảm bảo endTime >= startTime
                    val finalEnd = if (et.isBefore(st)) st.plusHours(1) else et

                    onSave(isAllowed, st, finalEnd, allowedDays)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                ),
                border = BorderStroke(1.dp, Primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Hủy", fontWeight = FontWeight.Medium)
            }
        }
    )
}
@Composable
fun TimeInputField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    labelWidth: Dp = 140.dp,
    fieldWidth: Dp = 100.dp,
    isEndTime: Boolean = false,
    startTime: TextFieldValue? = null, // dùng khi isEndTime = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(labelWidth)
        )

        Box(
            modifier = Modifier
                .width(fieldWidth)
                .height(36.dp)
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    val filtered = newValue.text.filter { it.isDigit() || it == ':' }
                    val formatted = autoFormatTime(filtered)
                    val newSelection = formatted.length.coerceAtMost(formatted.length)
                    onValueChange(
                        TextFieldValue(
                            text = formatted,
                            selection = TextRange(newSelection)
                        )
                    )
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color.Black),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 0.dp)
                    .onFocusChanged() { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            // mất focus
                            var text = value.text
                            if (text.isEmpty()) text = "23:59"

                            var parsed = runCatching { LocalTime.parse(text, formatter) }.getOrElse { LocalTime.of(23,59) }

                            if (isEndTime && startTime != null) {
                                val startParsed = runCatching { LocalTime.parse(startTime.text, formatter) }.getOrNull()
                                if (startParsed != null && parsed <= startParsed) {
                                    parsed = startParsed.plusMinutes(1).coerceAtMost(LocalTime.of(23,59))
                                }
                            }

                            onValueChange(TextFieldValue(text = parsed.format(formatter)))
                        }
                        isFocused = focusState.isFocused
                    },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            )
        }
    }
}

//
//@Preview(showBackground = true)
//@Composable
//fun AppSettingsDialogPreview() {
//    var showDialog by remember { mutableStateOf(true) }
//
//    if (showDialog) {
//        AppSettingsDialog(
//            appName = "YouTube",
//            initialIsAllowed = true,
//            initialStartTime = LocalTime.of(8, 0),
//            initialEndTime = LocalTime.of(20, 0),
//            initialAllowedDays = listOf("Mon","Tue","Wed","Thu","Fri"),
//            onDismiss = { showDialog = false },
//            onSave = { isAllowed, startTime, endTime, allowedDays ->
//                showDialog = false
//            }
//        )
//    }
//}
