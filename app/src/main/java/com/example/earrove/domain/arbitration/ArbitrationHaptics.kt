package com.example.earrove.domain.arbitration

/**
 * 仲裁器所需的震动能力抽象，便于单元测试注入 fake 实现。
 */
interface ArbitrationHaptics {
    fun vibrateForObstacle()
    fun vibrateForTurn(direction: String)
    fun vibrateForTrafficLight()
}

