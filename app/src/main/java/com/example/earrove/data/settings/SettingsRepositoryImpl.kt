package com.example.earrove.data.settings

import android.content.Context

class SettingsRepositoryImpl(
    context: Context
) : SettingsRepository {

    private val appContext = context.applicationContext

    private val prefs
        get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getTtsSpeechRate(): Float =
        prefs.getFloat(KEY_TTS_SPEECH_RATE, 1.0f)

    override fun setTtsSpeechRate(value: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEECH_RATE, value).apply()
    }

    override fun isHapticEnabled(): Boolean =
        prefs.getBoolean(KEY_HAPTIC_ENABLED, true)

    override fun setHapticEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()
    }

    override fun isVisualAssistEnabled(): Boolean =
        prefs.getBoolean(KEY_VISUAL_ASSIST_ENABLED, false)

    override fun setVisualAssistEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_VISUAL_ASSIST_ENABLED, value).apply()
    }

    override fun getHomeAddress(): String? {
        val value = prefs.getString(KEY_HOME_ADDRESS, null)?.trim()
        return if (value.isNullOrBlank()) null else value
    }

    override fun setHomeAddress(value: String) {
        prefs.edit().putString(KEY_HOME_ADDRESS, value.trim()).apply()
    }

    override fun clearHomeAddress() {
        prefs.edit().remove(KEY_HOME_ADDRESS).apply()
    }

    companion object {
        private const val PREFS_NAME = "earrove_settings"
        private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_VISUAL_ASSIST_ENABLED = "visual_assist_enabled"
        private const val KEY_HOME_ADDRESS = "home_address"
    }
}
