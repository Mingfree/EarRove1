package com.example.earrove.navigation

import com.example.earrove.domain.arbitration.ObstacleType
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

data class Obstacle(
    val type: ObstacleType,
    val distance: Int, // 米
    val direction: String
)

class ObstacleDetectionService {
    private val _currentObstacle = MutableStateFlow<Obstacle?>(null)
    private var isMonitoring = false

    val currentObstacle: StateFlow<Obstacle?> = _currentObstacle.asStateFlow()

    fun startMonitoring(): Flow<Obstacle?> = flow {
        isMonitoring = true
        while (isMonitoring) {
            // 随机生成障碍物（用于模拟测试）
            val obstacle = if (Random.nextBoolean() && Random.nextInt(10) > 7) {
                // 30%概率生成障碍物
                val types = ObstacleType.entries.filter { it != ObstacleType.NONE }
                val type = types[Random.nextInt(types.size)]
                val distance = Random.nextInt(1, 20)
                val directions = listOf("前方", "左前方", "右前方")
                val direction = directions[Random.nextInt(directions.size)]

                Obstacle(type, distance, direction)
            } else {
                null
            }

            _currentObstacle.value = obstacle
            emit(obstacle)

            // 每8秒检测一次
            delay(8000)
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        _currentObstacle.value = null
    }
}
