package com.example.earrove.domain.arbitration

/**
 * 送入仲裁器的统一业务事件（语音抢占与震动），避免分散调用 announce*。
 */
sealed class AccessibilityEvent {

    data class Obstacle(val obstacleType: String) : AccessibilityEvent()

    sealed class Nav : AccessibilityEvent() {
        data class Turn(val direction: String, val distanceMeters: Int) : Nav()
        data class TrafficLight(val status: String, val countdown: Int) : Nav()
        data class Destination(val name: String) : Nav()
        data class RouteStart(val destination: String, val distance: Int, val duration: Int) : Nav()
    }

    data class Ocr(val text: String) : AccessibilityEvent()

    data class Info(val text: String) : AccessibilityEvent()
}
