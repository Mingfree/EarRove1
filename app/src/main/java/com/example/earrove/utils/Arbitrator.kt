package com.example.earrove.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class Arbitrator(
    private val ttsManager: TTSManager,
    private val vibrationManager: VibrationManager
) {
    private val uiScope = CoroutineScope(Dispatchers.Main)

    enum class Priority {
        OBSTACLE,  // 障碍物检测（最高优先级）
        NAVIGATION, // 导航指令
        OCR,        // 文字识别
        INFO        // 普通信息
    }

    private var currentPriority = Priority.INFO
    private var isSpeaking = false

    fun speakWithPriority(text: String, priority: Priority, vibratePattern: (() -> Unit)? = null) {
        uiScope.launch {
            // 如果正在播报，且新消息优先级 >= 当前（ordinal 越小优先级越高），则中断旧播报
            if (isSpeaking && priority.ordinal <= currentPriority.ordinal) {
                ttsManager.stop()
            }

            // 更新当前优先级
            currentPriority = priority

            // 触发震动反馈
            vibratePattern?.invoke()

            // 播报语音并等待播放完成（使用真实 TTS 回调替代字数猜测延迟）
            isSpeaking = true
            ttsManager.speakAndWait(text)
            isSpeaking = false

            // 重置为默认优先级
            if (currentPriority == priority) {
                currentPriority = Priority.INFO
            }
        }
    }

    fun announceObstacle(obstacleType: String) {
        speakWithPriority(
            "前方${obstacleType}，请注意避让",
            Priority.OBSTACLE,
            { vibrationManager.vibrateForObstacle() }
        )
    }

    fun announceTurn(direction: String, distance: Int) {
        speakWithPriority(
            "前方${distance}米${direction}",
            Priority.NAVIGATION,
            { vibrationManager.vibrateForTurn(direction) }
        )
    }

    fun announceTrafficLight(status: String, countdown: Int) {
        speakWithPriority(
            "${status}，还有${countdown}秒",
            Priority.NAVIGATION,
            { vibrationManager.vibrateForTrafficLight() }
        )
    }

    fun announceDestination(destination: String) {
        speakWithPriority(
            "已到达${destination}",
            Priority.INFO,
            null
        )
    }

    fun announceRouteStart(destination: String, distance: Int, duration: Int) {
        speakWithPriority(
            "开始导航前往$destination，全程${distance}米，预计需要${duration}分钟",
            Priority.NAVIGATION,
            null
        )
    }
}

@Composable
fun rememberArbitrator(
    ttsManager: TTSManager,
    vibrationManager: VibrationManager
): Arbitrator {
    return remember {
        Arbitrator(ttsManager, vibrationManager)
    }
}