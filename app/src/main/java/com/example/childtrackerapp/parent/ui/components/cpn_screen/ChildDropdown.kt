package com.example.childtrackerapp.parent.ui.components.cpn_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.utils.BackgroundDefault
import com.example.childtrackerapp.utils.BackgroundSelected
import com.example.childtrackerapp.utils.Primary
import com.example.childtrackerapp.utils.TextPrimary
import com.example.childtrackerapp.utils.TextSecondary
import com.example.childtrackerapp.parent.ui.model.Child
import kotlin.collections.forEach


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDropdown(
    children: List<Child>,
    selectedChildId: String?,
    onChildSelected: (childId: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedText = children.firstOrNull { it.uid == selectedChildId }?.name ?: "Chọn con"
    var parentWidth by remember { mutableStateOf(0) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .onGloballyPositioned{ coordinates ->
            parentWidth = coordinates.size.width
        }
    ) {
        // Card chính của dropdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded },
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundDefault)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Dropdown",
                    tint = TextSecondary
                )
            }
        }

        // Menu dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { parentWidth.toDp() }),
            offset = DpOffset(x = 0.dp, y = 8.dp)
        ) {
            children.forEach { child ->
                val isSelected = child.uid == selectedChildId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp) // <-- tạo margin
                        .background(if (isSelected) BackgroundSelected else BackgroundDefault)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = child.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Primary else TextPrimary
                            )
                        },
                        modifier = Modifier
                            .background(if (isSelected) BackgroundSelected else BackgroundDefault),
                        onClick = {
                            onChildSelected(child.uid)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}