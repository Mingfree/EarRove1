package com.example.earrove.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.earrove.MyApplication
import com.example.earrove.data.privacy.PrivacyRepositoryImpl
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.domain.model.SettingsSnapshot
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class HomeAddressSaveResult {
    Saved,
    Empty,
    Failed
}

enum class HomeAddressClearResult {
    Cleared,
    NothingToClear,
    Failed
}

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val privacyInteractor: PrivacyConsentInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(settingsInteractor.loadSnapshot())
    val uiState: StateFlow<SettingsSnapshot> = _uiState.asStateFlow()

    fun onTtsSpeechRateChange(value: Float) {
        settingsInteractor.updateTtsSpeechRate(value)
        _uiState.update { it.copy(ttsSpeechRate = value) }
    }

    fun onHapticEnabledChange(value: Boolean) {
        settingsInteractor.updateHapticEnabled(value)
        _uiState.update { it.copy(hapticEnabled = value) }
    }

    fun onHomeAddressDraftChange(value: String) {
        _uiState.update { it.copy(homeAddress = value) }
    }

    fun saveHomeAddressFromDraft(): HomeAddressSaveResult {
        val addr = _uiState.value.homeAddress.trim()
        if (addr.isBlank()) return HomeAddressSaveResult.Empty
        return try {
            settingsInteractor.saveHomeAddress(addr)
            _uiState.update { it.copy(homeAddress = addr) }
            HomeAddressSaveResult.Saved
        } catch (_: Exception) {
            HomeAddressSaveResult.Failed
        }
    }

    fun clearHomeAddress(): HomeAddressClearResult {
        val hadSaved = !settingsInteractor.getHomeAddressOrNull().isNullOrBlank()
        val hasDraft = _uiState.value.homeAddress.isNotBlank()
        if (!hadSaved && !hasDraft) {
            return HomeAddressClearResult.NothingToClear
        }
        return try {
            settingsInteractor.clearHomeAddress()
            _uiState.update { it.copy(homeAddress = "") }
            HomeAddressClearResult.Cleared
        } catch (_: Exception) {
            HomeAddressClearResult.Failed
        }
    }

    fun revokeAllPrivacyAgreements() {
        privacyInteractor.revokeAllPrivacyAgreements()
    }

    companion object {
        fun factory(
            settingsInteractor: SettingsInteractor,
            privacyInteractor: PrivacyConsentInteractor
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsInteractor, privacyInteractor) as T
            }
        }

        fun defaultFactory(androidContext: android.content.Context): ViewModelProvider.Factory {
            val app = androidContext.applicationContext as? MyApplication
            val settings = app?.container?.settingsInteractor
                ?: SettingsInteractor(SettingsRepositoryImpl(androidContext.applicationContext))
            val privacy = app?.container?.privacyInteractor
                ?: PrivacyConsentInteractor(PrivacyRepositoryImpl(androidContext.applicationContext))
            return factory(settings, privacy)
        }
    }
}
