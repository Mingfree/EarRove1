package com.example.earrove.navigation

import com.example.earrove.domain.arbitration.TrafficLightStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class TrafficLight(
    val status: TrafficLightStatus,
    val countdown: Int, // 秒
    val distance: Int // 米
)

class TrafficLightService {
    private val _currentTrafficLight = MutableStateFlow<TrafficLight?>(null)
    private var isMonitoring = false

    val currentTrafficLight: StateFlow<TrafficLight?> = _currentTrafficLight.asStateFlow()

    fun startMonitoring(): Flow<TrafficLight?> = flow {
        isMonitoring = true
        while (isMonitoring) {
            // 模拟红绿灯检测（当距离路口小于50米时）
            val trafficLight = if (Random.nextBoolean() && Random.nextInt(10) > 7) {
                // 20%概率检测到红绿灯
                val status = when (Random.nextInt(3)) {
                    0 -> TrafficLightStatus.RED
                    1 -> TrafficLightStatus.GREEN
                    else -> TrafficLightStatus.YELLOW
                }
                val countdown = Random.nextInt(5, 30)
                val distance = Random.nextInt(10, 50)

                TrafficLight(status, countdown, distance)
            } else {
                null
            }

            _currentTrafficLight.value = trafficLight
            emit(trafficLight)

            // 每15秒检测一次
            delay(15000)
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        _currentTrafficLight.value = null
    }
}