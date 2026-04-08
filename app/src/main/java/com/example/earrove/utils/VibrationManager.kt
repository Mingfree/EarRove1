package com.example.earrove.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class VibrationManager(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrateForObstacle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 100, 100) // 短震2次
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
    }

    fun vibrateForTurn(direction: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 长震1次，配合语音提示
            vibrator.vibrate(VibrationEffect.createOneShot(500, 255))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    fun vibrateForTrafficLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 100, 100, 100, 100) // 三次短震
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
        }
    }

    fun cancel() {
        vibrator.cancel()
    }
}

@Composable
fun rememberVibrationManager(): VibrationManager {
    val context = LocalContext.current
    return remember {
        VibrationManager(context)
    }
}