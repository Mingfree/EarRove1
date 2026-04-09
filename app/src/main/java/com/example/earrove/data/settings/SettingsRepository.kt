package com.example.earrove.data.settings

/**
 * 应用设置持久化抽象，实现类负责 SharedPreferences / DataStore 等细节。
 */
interface SettingsRepository {
    fun getTtsSpeechRate(): Float
    fun setTtsSpeechRate(value: Float)

    fun isHapticEnabled(): Boolean
    fun setHapticEnabled(value: Boolean)

    fun isVisualAssistEnabled(): Boolean
    fun setVisualAssistEnabled(value: Boolean)

    fun getHomeAddress(): String?
    fun setHomeAddress(value: String)
    fun clearHomeAddress()
}
