package com.example.earrove.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.earrove.domain.arbitration.AccessibilityEvent
import com.example.earrove.domain.arbitration.ArbitrationHaptics
import com.example.earrove.domain.arbitration.ArbitrationSpeech
import com.example.earrove.domain.arbitration.ArbitrationTextProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Arbitrator(
    private val textProvider: ArbitrationTextProvider,
    private val speech: ArbitrationSpeech,
    private val haptics: ArbitrationHaptics,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val uiScope = CoroutineScope(SupervisorJob() + dispatcher)

    enum class Priority {
        OBSTACLE,
        NAVIGATION,
        OCR,
        INFO
    }

    private var currentPriority = Priority.INFO
    private var isSpeaking = false

    fun speakWithPriority(text: String, priority: Priority, vibratePattern: (() -> Unit)? = null) {
        uiScope.launch {
            if (isSpeaking && priority.ordinal <= currentPriority.ordinal) {
                speech.stop()
            }

            currentPriority = priority

            vibratePattern?.invoke()

            isSpeaking = true
            speech.speakAndWait(text)
            isSpeaking = false

            if (currentPriority == priority) {
                currentPriority = Priority.INFO
            }
        }
    }

    /**
     * 统一入口：业务侧只构造 [AccessibilityEvent]，仲裁与播报规则集中在此。
     */
    fun submit(event: AccessibilityEvent) {
        when (event) {
            is AccessibilityEvent.Obstacle -> speakWithPriority(
                textProvider.obstacleText(event.obstacleType),
                Priority.OBSTACLE,
                { haptics.vibrateForObstacle() }
            )

            is AccessibilityEvent.Nav.Turn -> speakWithPriority(
                textProvider.turnText(event.distanceMeters, event.direction),
                Priority.NAVIGATION,
                { haptics.vibrateForTurn(event.direction) }
            )

            is AccessibilityEvent.Nav.TrafficLight -> speakWithPriority(
                textProvider.trafficLightText(event.status, event.countdown),
                Priority.NAVIGATION,
                { haptics.vibrateForTrafficLight() }
            )

            is AccessibilityEvent.Nav.Destination -> speakWithPriority(
                textProvider.destinationText(event.name),
                Priority.INFO,
                null
            )

            is AccessibilityEvent.Nav.RouteStart -> speakWithPriority(
                textProvider.routeStartText(event.destination, event.distance, event.duration),
                Priority.NAVIGATION,
                null
            )

            is AccessibilityEvent.Ocr -> speakWithPriority(
                event.text,
                Priority.OCR,
                null
            )

            is AccessibilityEvent.Info -> speakWithPriority(
                event.text,
                Priority.INFO,
                null
            )
        }
    }
}

@Composable
fun rememberArbitrator(
    context: Context,
    ttsManager: TTSManager,
    vibrationManager: VibrationManager
): Arbitrator {
    return remember {
        Arbitrator(
            textProvider = AndroidArbitrationTextProvider(context),
            speech = ttsManager,
            haptics = vibrationManager
        )
    }
}
