package com.example.childtrackerapp.parent.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.utils.BackgroundSelected
import com.example.childtrackerapp.utils.Primary
import com.example.childtrackerapp.utils.PrimaryLight
import com.example.childtrackerapp.parent.ui.components.cpn_screen.AppItem
import com.example.childtrackerapp.parent.ui.components.cpn_screen.ChildDropdown
import com.example.childtrackerapp.parent.ui.components.cpn_screen.StatisticsTab
import com.example.childtrackerapp.parent.ui.model.UsageFilter
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsUiState
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsScreen(
    viewModel: AllowedAppsViewModel,
    uiState: AllowedAppsUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
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
                                    colors = listOf(PrimaryLight, Primary)
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
                            .background(if (tabSelected) BackgroundSelected else Color.Transparent)
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
                                color = if (tabSelected) Primary else Color.Gray
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
                                AppItem(
                                    app = app,
                                    onSaveSettings = { isAllowed, startTime, endTime, allowedDays ->
                                        val updatedApp = app.copy(
                                            allowed = isAllowed,
                                            startTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                            endTime = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                            allowedDays = allowedDays
                                        )

                                        // Gọi coroutine để update Firebase
                                        viewModel.updateAppSettings(updatedApp) { success ->
                                            if (success) Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                                            else Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                                        }
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
                            selectedFilter = uiState.filter,
                            onFilterChange = { filterStr ->
                                val usageFilter = when (filterStr) {
                                    "Ngày" -> UsageFilter.DAY
                                    "Tuần" -> UsageFilter.WEEK
                                    "Tháng" -> UsageFilter.MONTH
                                    else -> UsageFilter.DAY
                                }
                                viewModel.onFilterChanged(usageFilter)
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

