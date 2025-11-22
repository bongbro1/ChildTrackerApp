package com.example.childtrackerapp.utils

import com.example.childtrackerapp.parent.ui.model.UsageFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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

    val usageDate = parseStrToDate(usageTimeStr) // parse ra Date
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
fun parseStrToDate(usageTimeStr: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(usageTimeStr) ?: Date(0)
    } catch (e: Exception) {
        Date(0)
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

// nhập số tự chuyển sang time
fun autoFormatTime(input: String): String {
    val digits = input.filter { it.isDigit() }

    return when (digits.length) {
        0 -> ""
        1 -> digits // vẫn cho phép nhập 1 chữ số đầu
        2 -> {
            // 2 chữ số đầu → giờ
            val hour = digits.toInt().coerceIn(0, 23)
            "%02d".format(hour)
        }
        3 -> {
            // 2 chữ số giờ + 1 chữ số phút đầu, chưa ép full phút
            val hour = digits.substring(0, 2).toInt().coerceIn(0, 23)
            val minuteFirst = digits[2] // chưa ép 0-5 để người dùng nhập tiếp tự nhiên
            "%02d:%s".format(hour, minuteFirst)
        }
        else -> {
            // 2 chữ số giờ + 2 chữ số phút
            var hour = digits.substring(0, 2).toInt()
            var minute = digits.substring(2, 4).toInt()
            if (hour > 23) hour = 23
            if (minute > 59) minute = 59
            "%02d:%02d".format(hour, minute)
        }
    }
}
