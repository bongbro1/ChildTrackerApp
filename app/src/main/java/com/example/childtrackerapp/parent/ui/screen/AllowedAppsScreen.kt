package com.example.childtrackerapp.parent.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.childtrackerapp.parent.ui.model.AppInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import com.example.childtrackerapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.helpers.theme.BackgroundActive
import com.example.childtrackerapp.helpers.theme.Blue400
import com.example.childtrackerapp.helpers.theme.Blue600
import com.example.childtrackerapp.helpers.theme.Blue700
import com.example.childtrackerapp.helpers.theme.Blue900
import com.example.childtrackerapp.parent.helper.toImageBitmapOrNull
import com.example.childtrackerapp.parent.ui.model.AppUsage
import com.example.childtrackerapp.parent.ui.model.Child
import com.example.childtrackerapp.parent.ui.model.UsageFilter
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsUiState
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsScreen(
    viewModel: AllowedAppsViewModel,
    uiState: AllowedAppsUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(1) } // 0: Apps, 1: Web
    val tabs = listOf("Ứng dụng", "Thống kê")


    val children = uiState.children
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    val animatedTabIndex by animateIntAsState(targetValue = selectedTabIndex)

    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            selectedChildId = children.first().uid
            viewModel.onChildSelected(selectedChildId!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChildDropdown(
            children = children,
            selectedChildId = selectedChildId,
            onChildSelected = { childId ->
                selectedChildId = childId
                // Làm gì đó khi chọn child
                viewModel.onChildSelected(childId)
            }
        )

        // Tab menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) // padding trái phải cho TabRow
        ) {
            TabRow(
                selectedTabIndex = animatedTabIndex,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    val currentTabPosition = tabPositions[animatedTabIndex]
                    Box(
                        Modifier
                            .tabIndicatorOffset(currentTabPosition)
                            .height(4.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Blue400, Blue600)
                                ),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val tabSelected = selectedTabIndex == index
                    val interactionSource = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (tabSelected) BackgroundActive else Color.Transparent)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                selectedTabIndex = index
                            },
                        color = Color.Transparent
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                title,
                                fontWeight = if (tabSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (tabSelected) Blue600 else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (selectedTabIndex) {
            0 -> { // Tab Apps
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.apps.isEmpty()) {
                        // Hiển thị khi không có app
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Không có ứng dụng nào")
                        }
                    }  else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.apps) { app ->
                                BlockedAppItem(
                                    app = app,
                                    onToggle = { allowed ->
                                        viewModel.toggleApp(selectedChildId as String, app.packageName, allowed)
                                        Toast.makeText(
                                            context,
                                            "${app.name} đã ${if (allowed) "cho phép" else "chặn"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {

                if (uiState.statisticsLoading) {
                    // nếu đang load data thống kê
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                } else {
                    if (selectedChildId != null) {
                        StatisticsTab(
                            childName = uiState.selectedChildName ?: "Không rõ",
                            dateString = uiState.currentDateString,
                            totalMinutes = uiState.totalUsageMinutes,
                            topApps = uiState.topApps,
                            allApps = uiState.allApps,
                            onFilterChange = { filterStr ->
                                val filter = when (filterStr) {
                                    "Ngày" -> UsageFilter.DAY
                                    "Tuần" -> UsageFilter.WEEK
                                    "Tháng" -> UsageFilter.MONTH
                                    else -> UsageFilter.DAY
                                }
                                viewModel.onFilterChanged(filter)
                            }
                        )
                    }
                    else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Vui lòng chọn con để xem thống kê")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BlockedAppItem(app: AppInfo, onToggle: (Boolean) -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val iconBitmap = app.iconBase64.toImageBitmapOrNull()
            // Icon app
            iconBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            } ?: Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp)
            )// placeholder nếu không có icon

            // Tên app
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Tên app
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = app.usageTime ?: "0m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Switch
            Switch(
                checked = app.isAllowed,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDropdown(
    children: List<Child>,
    selectedChildId: String?,
    onChildSelected: (childId: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // selectedText sẽ tự động cập nhật khi selectedChildId hoặc children thay đổi
    val selectedText = remember(selectedChildId, children) {
        children.firstOrNull { it.uid == selectedChildId }?.name ?: "Chọn child"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                children.forEach { child ->
                    DropdownMenuItem(
                        text = { Text(child.name) },
                        onClick = {
                            expanded = false
                            onChildSelected(child.uid)
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun StatisticsTab(
    childName: String,
    dateString: String,
    totalMinutes: Int,
    topApps: List<AppUsage>,
    allApps: List<AppUsage>,
    onFilterChange: (String) -> Unit
) {

    val optionsFilter = listOf("Ngày", "Tuần", "Tháng")
    var selectedFilter by remember { mutableStateOf("Ngày") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ===== HEADER =====
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = childName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Blue900
                        )
                        Text(
                            text = dateString,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Blue700,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tổng thời gian: ${formatDuration(totalMinutes)}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Blue900
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // ===== FILTER =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                optionsFilter.forEach { label ->
                    val isSelected = label == selectedFilter // selectedFilter là state hiện tại
                    Button(
                        onClick = { onFilterChange(label) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Blue600 else Color.White,
                            contentColor = if (isSelected) Color.White else Blue600
                        ),
                        border = if (!isSelected) BorderStroke(1.dp, Blue600) else null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // ===== TOP APPS BAR CHART =====
        item {
            Text("Top 5 ứng dụng", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (topApps.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Không có dữ liệu")
                    }
                } else {
                    BarChart(topApps)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ===== DANH SÁCH CHI TIẾT =====
        item {
            Text("Chi tiết tất cả ứng dụng", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(allApps) { app ->
            AppUsageItem(app)
        }
    }
}

@Composable
fun BarChart(apps: List<AppUsage>) {
    val maxTime = (apps.maxOfOrNull { it.timeMinutes } ?: 1).toFloat()

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        apps.forEach { app ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.name,
                    modifier = Modifier.width(90.dp)
                )

                val percent = (app.timeMinutes / maxTime)

                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .weight(1f)
                        .background(Color.LightGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percent)
                            .background(Color(0xFF4CAF50))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(app.formatted)
            }
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.name, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(app.formatted)
    }
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

