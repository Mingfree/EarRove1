package com.example.earrove.domain.usecase.settings

import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.domain.model.SettingsSnapshot

/**
 * 设置相关读写的用例入口，UI 与工具类通过此类访问 [SettingsRepository]，便于 mock。
 */
class SettingsInteractor(
    private val settingsRepository: SettingsRepository
) {
    fun loadSnapshot(): SettingsSnapshot = SettingsSnapshot(
        ttsSpeechRate = settingsRepository.getTtsSpeechRate(),
        hapticEnabled = settingsRepository.isHapticEnabled(),
        visualAssistEnabled = settingsRepository.isVisualAssistEnabled(),
        homeAddress = settingsRepository.getHomeAddress().orEmpty()
    )

    fun updateTtsSpeechRate(value: Float) {
        settingsRepository.setTtsSpeechRate(value)
    }

    fun updateHapticEnabled(value: Boolean) {
        settingsRepository.setHapticEnabled(value)
    }

    fun updateVisualAssistEnabled(value: Boolean) {
        settingsRepository.setVisualAssistEnabled(value)
    }

    fun saveHomeAddress(trimmed: String) {
        if (trimmed.isNotBlank()) {
            settingsRepository.setHomeAddress(trimmed)
        }
    }

    fun clearHomeAddress() {
        settingsRepository.clearHomeAddress()
    }

    fun getHomeAddressOrNull(): String? = settingsRepository.getHomeAddress()
}
