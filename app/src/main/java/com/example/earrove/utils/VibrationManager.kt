package com.example.earrove.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.earrove.MyApplication
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.domain.arbitration.ArbitrationHaptics

class VibrationManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) : ArbitrationHaptics {
    constructor(context: Context) : this(context, SettingsRepositoryImpl(context))

    private val appContext = context.applicationContext
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun vibrateForObstacle() {
        if (!settingsRepository.isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 100, 100) // 短震2次
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
    }

    override fun vibrateForTurn(direction: String) {
        if (!settingsRepository.isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 长震1次，配合语音提示
            vibrator.vibrate(VibrationEffect.createOneShot(500, 255))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    override fun vibrateForTrafficLight() {
        if (!settingsRepository.isHapticEnabled()) return
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
    val app = context.applicationContext as? MyApplication
    return remember(context) {
        VibrationManager(context, app?.container?.settingsRepository ?: SettingsRepositoryImpl(context))
    }
}