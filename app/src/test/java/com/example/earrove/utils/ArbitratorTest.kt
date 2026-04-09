package com.example.earrove.utils

import com.example.earrove.domain.arbitration.AccessibilityEvent
import com.example.earrove.domain.arbitration.ArbitrationHaptics
import com.example.earrove.domain.arbitration.ArbitrationSpeech
import com.example.earrove.domain.arbitration.ArbitrationTextProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArbitratorTest {

    private class FakeTextProvider : ArbitrationTextProvider {
        override fun obstacleText(obstacleType: String) = "obstacle:$obstacleType"
        override fun turnText(distanceMeters: Int, direction: String) = "turn:$distanceMeters:$direction"
        override fun trafficLightText(status: String, countdown: Int) = "light:$status:$countdown"
        override fun destinationText(name: String) = "dest:$name"
        override fun routeStartText(destination: String, distance: Int, durationMinutes: Int) =
            "route:$destination:$distance:$durationMinutes"
    }

    private class FakeHaptics : ArbitrationHaptics {
        var obstacleCount = 0
        var turnCount = 0
        var trafficCount = 0

        override fun vibrateForObstacle() {
            obstacleCount++
        }

        override fun vibrateForTurn(direction: String) {
            turnCount++
        }

        override fun vibrateForTrafficLight() {
            trafficCount++
        }
    }

    private class BlockingSpeech : ArbitrationSpeech {
        val spoken = mutableListOf<String>()
        var stopCount = 0
        val gate = CompletableDeferred<Unit>()

        override suspend fun speakAndWait(text: String) {
            spoken.add(text)
            gate.await()
        }

        override fun stop() {
            stopCount++
        }
    }

    @Test
    fun higherPriority_interruptsLowerPriority() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val speech = BlockingSpeech()
        val haptics = FakeHaptics()
        val arbitrator = Arbitrator(
            textProvider = FakeTextProvider(),
            speech = speech,
            haptics = haptics,
            dispatcher = dispatcher
        )

        arbitrator.submit(AccessibilityEvent.Info("info"))
        testScheduler.runCurrent()

        arbitrator.submit(AccessibilityEvent.Obstacle("barrier"))
        testScheduler.runCurrent()

        assertTrue("应该触发抢占 stop()", speech.stopCount >= 1)
        assertEquals(1, haptics.obstacleCount)

        speech.gate.complete(Unit)
        testScheduler.advanceUntilIdle()
    }
}

