package com.example.childtrackerapp.child.helper

import android.content.Context
import android.location.Location

object GeoFenceHelper {

    private val dangerZones = listOf(
        Pair(21.5665, 105.7274), // Hồ Núi Cốc
        Pair(21.5985, 105.8557), // Sông Cầu
        Pair(21.6468, 105.9075), // Đập nước Đồng Hỷ
        Pair(21.5861, 105.8580), // Cầu Bến Tượng
        Pair(21.5898, 105.8276), // Hồ Xương Rồng
        Pair(21.4431, 105.7632), // Hồ Suối Lạnh
        Pair(21.9311, 105.7348), // Đèo De
        Pair(21.7258, 105.6549), // Hồ Vai Miếu
        Pair(21.584098616776586, 105.80729206616967)
    )

    private const val DANGER_RADIUS = 600.0
    private const val ALERT_COUNT = 3

    var voiceAlert: VoiceAlertHelper? = null
    private val hasAlerted = mutableMapOf<Pair<Double, Double>, Int>()

    fun init(context: Context) {
        if (voiceAlert == null) voiceAlert = VoiceAlertHelper(context)
    }

    fun checkDangerZone(context: Context, location: Location) {
        init(context)

        for (zone in dangerZones) {
            val loc = Location("").apply {
                latitude = zone.first
                longitude = zone.second
            }
            val distance = location.distanceTo(loc)

            if (distance < DANGER_RADIUS) {
                val count = hasAlerted.getOrDefault(zone, 0)
                if (count < ALERT_COUNT) {
                    voiceAlert?.speak("Cảnh báo! Bạn đang tiến gần khu vực nguy hiểm, hãy quay lại!")
                    hasAlerted[zone] = count + 1
                }
            } else {
                // reset count khi rời khỏi zone
                hasAlerted[zone] = 0
            }
        }
    }

    fun release() {
        voiceAlert?.release()
        voiceAlert = null
    }
}
