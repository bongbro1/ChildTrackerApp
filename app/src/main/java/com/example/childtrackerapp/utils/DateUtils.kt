package com.example.childtrackerapp.utils

import com.example.childtrackerapp.parent.ui.model.UsageFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


fun parseUsageTimeToMinutes(usageTime: String): Int {
    // Loại bỏ khoảng trắng
    val normalized = usageTime.replace(" ", "")

    val hRegex = "(\\d+)h".toRegex()
    val mRegex = "(\\d+)m".toRegex()

    val hours = hRegex.find(normalized)?.groups?.get(1)?.value?.toIntOrNull() ?: 0
    val minutes = mRegex.find(normalized)?.groups?.get(1)?.value?.toIntOrNull() ?: 0

    return hours * 60 + minutes
}
fun isWithinFilter(usageTimeStr: String?, filter: UsageFilter): Boolean {
    if (usageTimeStr.isNullOrEmpty()) return false

    val usageDate = parseUsageTimeToDate(usageTimeStr) // parse ra Date
    val now = Calendar.getInstance()

    return when (filter) {
        UsageFilter.DAY -> isSameDay(now.time, usageDate)
        UsageFilter.WEEK -> isSameWeek(now.time, usageDate)
        UsageFilter.MONTH -> isSameMonth(now.time, usageDate)
    }
}

fun getDateString(filter: UsageFilter): String {
    val now = Date()
    val formatter = SimpleDateFormat(
        when (filter) {
            UsageFilter.DAY -> "yyyy-MM-dd"
            UsageFilter.WEEK -> "'Tuần' w, yyyy"
            UsageFilter.MONTH -> "MMMM yyyy"
        }, Locale.getDefault()
    )
    return formatter.format(now)
}
fun parseUsageTimeToDate(usageTimeStr: String): Date {
    return try {
        // Giả sử usageTimeStr là "yyyy-MM-dd HH:mm:ss"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.parse(usageTimeStr) ?: Date(0)
    } catch (e: Exception) {
        Date(0) // trả về epoch nếu parse lỗi
    }
}


fun isSameDay(today: Date, usageDate: Date): Boolean {
    val calToday = Calendar.getInstance().apply { time = today }
    val calUsage = Calendar.getInstance().apply { time = usageDate }
    return calToday.get(Calendar.YEAR) == calUsage.get(Calendar.YEAR)
            && calToday.get(Calendar.DAY_OF_YEAR) == calUsage.get(Calendar.DAY_OF_YEAR)
}

fun isSameWeek(today: Date, usageDate: Date): Boolean {
    val calToday = Calendar.getInstance().apply { time = today }
    val calUsage = Calendar.getInstance().apply { time = usageDate }
    return calToday.get(Calendar.YEAR) == calUsage.get(Calendar.YEAR)
            && calToday.get(Calendar.WEEK_OF_YEAR) == calUsage.get(Calendar.WEEK_OF_YEAR)
}

fun isSameMonth(today: Date, usageDate: Date): Boolean {
    val calToday = Calendar.getInstance().apply { time = today }
    val calUsage = Calendar.getInstance().apply { time = usageDate }
    return calToday.get(Calendar.YEAR) == calUsage.get(Calendar.YEAR)
            && calToday.get(Calendar.MONTH) == calUsage.get(Calendar.MONTH)
}
