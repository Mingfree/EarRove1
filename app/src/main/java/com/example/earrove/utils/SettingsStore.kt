package com.example.earrove.utils

import android.content.Context

/**
 * 简易设置存储层（先用 SharedPreferences，后续可再抽成 repository/domain）。
 */
object SettingsStore {
    private const val PREFS_NAME = "earrove_settings"

    private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
    private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
    private const val KEY_VISUAL_ASSIST_ENABLED = "visual_assist_enabled"

    fun getTtsSpeechRate(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_TTS_SPEECH_RATE, 1.0f)
    }

    fun setTtsSpeechRate(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_TTS_SPEECH_RATE, value)
            .apply()
    }

    fun isHapticEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTIC_ENABLED, value)
            .apply()
    }

    fun isVisualAssistEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VISUAL_ASSIST_ENABLED, false)
    }

    fun setVisualAssistEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VISUAL_ASSIST_ENABLED, value)
            .apply()
    }
}

