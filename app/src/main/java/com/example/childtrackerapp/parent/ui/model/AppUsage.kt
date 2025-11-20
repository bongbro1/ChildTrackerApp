package com.example.childtrackerapp.parent.ui.model

data class AppUsage(
    val packageName: String,
    val name: String,
    val timeMinutes: Int,
) {
    val formatted: String
        get() {
            val h = timeMinutes / 60
            val m = timeMinutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
}

enum class UsageFilter {
    DAY,
    WEEK,
    MONTH
}
data class UsageStats(
    val childName: String,
    val dateString: String,
    val totalMinutes: Int,
    val topApps: List<AppUsage>,
    val allApps: List<AppUsage>
)

