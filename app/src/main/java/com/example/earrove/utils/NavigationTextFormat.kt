package com.example.earrove.utils

/**
 * 导航距离/时长的统一格式化，保证界面显示与语音播报口径一致。
 */
object NavigationTextFormat {

    /** 距离简写：>=1000 米显示为公里（保留一位小数），否则显示米 */
    fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f公里", distanceMeters / 1000f)
        } else {
            "${distanceMeters}米"
        }
    }

    /** 秒级时长：不足 1 分钟显示秒，否则转分钟 */
    fun formatDurationSeconds(seconds: Int): String {
        if (seconds < 60) return "${seconds}秒"
        return formatDurationMinutes(seconds / 60)
    }

    /** 分钟级时长：不足 1 小时显示分钟，否则显示「x小时y分钟」 */
    fun formatDurationMinutes(minutes: Int): String {
        if (minutes < 60) return "${minutes}分钟"
        val hours = minutes / 60
        val remain = minutes % 60
        return if (remain == 0) "${hours}小时" else "${hours}小时${remain}分钟"
    }
}
