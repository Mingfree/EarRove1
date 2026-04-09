package com.example.earrove.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.earrove.R
import com.example.earrove.domain.arbitration.AccessibilityEvent
import com.example.earrove.domain.arbitration.ArbitrationSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Arbitrator(
    private val context: Context,
    private val speech: ArbitrationSpeech,
    private val vibrationManager: VibrationManager
) {
    private val uiScope = CoroutineScope(Dispatchers.Main)

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
                context.getString(R.string.arb_obstacle_template, event.obstacleType),
                Priority.OBSTACLE,
                { vibrationManager.vibrateForObstacle() }
            )

            is AccessibilityEvent.Nav.Turn -> speakWithPriority(
                context.getString(R.string.arb_turn_template, event.distanceMeters, event.direction),
                Priority.NAVIGATION,
                { vibrationManager.vibrateForTurn(event.direction) }
            )

            is AccessibilityEvent.Nav.TrafficLight -> speakWithPriority(
                context.getString(R.string.arb_traffic_light_template, event.status, event.countdown),
                Priority.NAVIGATION,
                { vibrationManager.vibrateForTrafficLight() }
            )

            is AccessibilityEvent.Nav.Destination -> speakWithPriority(
                context.getString(R.string.arb_destination_template, event.name),
                Priority.INFO,
                null
            )

            is AccessibilityEvent.Nav.RouteStart -> speakWithPriority(
                context.getString(
                    R.string.arb_route_start_template,
                    event.destination,
                    event.distance,
                    event.duration
                ),
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
        Arbitrator(context, ttsManager, vibrationManager)
    }
}
