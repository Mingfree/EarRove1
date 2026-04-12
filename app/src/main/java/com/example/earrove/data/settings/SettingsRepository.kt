package com.example.earrove.data.settings

/**
 * 应用设置持久化抽象，实现类负责 SharedPreferences / DataStore 等细节。
 */
interface SettingsRepository {
    fun getTtsSpeechRate(): Float
    fun setTtsSpeechRate(value: Float)

    fun isHapticEnabled(): Boolean
    fun setHapticEnabled(value: Boolean)

    /** 是否在导航中播放模拟的红绿灯与避障提示（默认关闭） */
    fun isSimulatedNavAlertsEnabled(): Boolean
    fun setSimulatedNavAlertsEnabled(value: Boolean)

    fun getHomeAddress(): String?
    fun setHomeAddress(value: String)
    fun clearHomeAddress()
}
