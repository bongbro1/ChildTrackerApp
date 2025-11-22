package com.example.childtrackerapp.parent.ui.components.cpn_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childtrackerapp.utils.Primary
import com.example.childtrackerapp.utils.PrimaryDark
import com.example.childtrackerapp.utils.PrimaryDarker
import com.example.childtrackerapp.parent.ui.model.AppUsage
import com.example.childtrackerapp.parent.ui.model.UsageFilter
import kotlin.collections.forEach
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.example.childtrackerapp.utils.BackgroundDefault
import com.example.childtrackerapp.utils.BackgroundSecondary
import com.example.childtrackerapp.utils.BackgroundSelected
import com.example.childtrackerapp.utils.PrimaryLight


@Composable
fun StatisticsTab(
    childName: String,
    dateString: String,
    totalMinutes: Int,
    topApps: List<AppUsage>,
    allApps: List<AppUsage>,
    selectedFilter: UsageFilter,
    onFilterChange: (String) -> Unit
) {

    val optionsFilter = listOf("Ngày", "Tuần", "Tháng")
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ===== HEADER =====
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundSelected),
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
                            color = PrimaryDarker
                        )
                        Text(
                            text = dateString,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = PrimaryDark,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tổng thời gian: ${formatDuration(totalMinutes)}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = PrimaryDarker
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
                    val filterForLabel = when (label) {
                        "Ngày" -> UsageFilter.DAY
                        "Tuần" -> UsageFilter.WEEK
                        "Tháng" -> UsageFilter.MONTH
                        else -> UsageFilter.DAY
                    }
                    val isSelected = filterForLabel == selectedFilter
                    Button(
                        onClick = {
                            onFilterChange(label)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Primary else Color.White,
                            contentColor = if (isSelected) Color.White else Primary
                        ),
                        border = if (!isSelected) BorderStroke(1.dp, Primary) else null,
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
                colors = CardDefaults.cardColors(containerColor = BackgroundSelected),
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
        if (!topApps.isEmpty()) {
            item {
                Text("Chi tiết tất cả ứng dụng", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allApps) { app ->
                AppUsageItem(app)
            }
        }
    }
}
@Composable
fun BarChart(apps: List<AppUsage>) {
    val maxTime = (apps.maxOfOrNull { it.timeMinutes } ?: 1).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        apps.forEach { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tên app
                Text(
                    text = app.name,
                    modifier = Modifier.width(70.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Thanh bar
                val percent = (app.timeMinutes / maxTime)
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .weight(1f)
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percent)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Primary, PrimaryLight)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Thời gian sử dụng
                Text(
                    text = app.formatted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.width(50.dp)
                )
            }
        }
    }
}
@Composable
fun AppUsageItem(app: AppUsage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tên app linh hoạt, không xuống dòng
            Text(
                text = app.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Thời gian sử dụng
            Text(
                text = app.formatted,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Mock data
val sampleTopApps = listOf(
    AppUsage("com.youtube.android", "YouTube", 120),
    AppUsage("com.zhiliaoapp.musically", "TikTok", 90),
    AppUsage("com.instagram.android", "Instagram", 60),
    AppUsage("com.facebook.orca", "Messenger", 45),
    AppUsage("com.android.chrome", "Chrome", 30)
)

val sampleAllApps = listOf(
    AppUsage("com.youtube.android", "YouTube", 120),
    AppUsage("com.zhiliaoapp.musically", "TikTok", 90),
    AppUsage("com.instagram.android", "Instagram", 60),
    AppUsage("com.facebook.orca", "Messenger", 45),
    AppUsage("com.android.chrome", "Chrome", 30),
    AppUsage("com.facebook.katana", "Facebook", 25),
    AppUsage("com.spotify.music", "Spotify", 20)
)

@Preview(showBackground = true)
@Composable
fun StatisticsTabPreview() {
    var selectedFilter by remember { mutableStateOf(UsageFilter.DAY) }

    StatisticsTab(
        childName = "Bé An",
        dateString = "21/11/2025",
        totalMinutes = 390,
        topApps = sampleTopApps,
        allApps = sampleAllApps,
        selectedFilter = selectedFilter,
        onFilterChange = { label ->
            selectedFilter = when (label) {
                "Ngày" -> UsageFilter.DAY
                "Tuần" -> UsageFilter.WEEK
                "Tháng" -> UsageFilter.MONTH
                else -> UsageFilter.DAY
            }
        }
    )
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
