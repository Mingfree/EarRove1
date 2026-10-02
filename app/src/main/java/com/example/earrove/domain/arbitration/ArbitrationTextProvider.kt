package com.example.earrove.domain.arbitration

/**
 * 仲裁器所需的文案提供者抽象，避免单元测试依赖 Android Context/R.string。
 */
interface ArbitrationTextProvider {
    fun obstacleText(obstacleType: ObstacleType): String
    fun turnText(distanceMeters: Int, turnType: TurnDirection): String
    fun trafficLightText(status: TrafficLightStatus, countdown: Int): String
    fun destinationText(name: String): String
    fun routeStartText(destination: String, distance: Int, durationMinutes: Int): String
}
