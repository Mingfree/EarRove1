package com.example.earrove.domain.arbitration

/**
 * 仲裁器所需的文案提供者抽象，避免单元测试依赖 Android Context/R.string。
 */
interface ArbitrationTextProvider {
    fun obstacleText(obstacleType: String): String
    fun turnText(distanceMeters: Int, direction: String): String
    fun trafficLightText(status: String, countdown: Int): String
    fun destinationText(name: String): String
    fun routeStartText(destination: String, distance: Int, durationMinutes: Int): String
}

