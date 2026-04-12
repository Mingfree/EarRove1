package com.example.earrove.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.earrove.MyApplication
import com.example.earrove.data.privacy.PrivacyRepositoryImpl
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.domain.model.SettingsSnapshot
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import com.example.earrove.domain.validation.HomeAddressGeocodeOutcome
import com.example.earrove.domain.validation.HomeAddressGeocodeVerifier
import com.example.earrove.domain.validation.HomeAddressInputRules
import com.example.earrove.utils.BaiduHomeAddressGeocodeVerifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeAddressSaveResult {
    Saved,
    Empty,
    InvalidFormat,
    NotRecognizedOnMap,
    GeocodeError,
    Failed
}

enum class HomeAddressClearResult {
    Cleared,
    NothingToClear,
    Failed
}

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val privacyInteractor: PrivacyConsentInteractor,
    private val homeAddressGeocodeVerifier: HomeAddressGeocodeVerifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(settingsInteractor.loadSnapshot())
    val uiState: StateFlow<SettingsSnapshot> = _uiState.asStateFlow()

    private val _homeAddressSaveInProgress = MutableStateFlow(false)
    val homeAddressSaveInProgress: StateFlow<Boolean> = _homeAddressSaveInProgress.asStateFlow()

    fun onTtsSpeechRateChange(value: Float) {
        settingsInteractor.updateTtsSpeechRate(value)
        _uiState.update { it.copy(ttsSpeechRate = value) }
    }

    fun onHapticEnabledChange(value: Boolean) {
        settingsInteractor.updateHapticEnabled(value)
        _uiState.update { it.copy(hapticEnabled = value) }
    }

    fun onSimulatedNavAlertsChange(value: Boolean) {
        settingsInteractor.updateSimulatedNavAlertsEnabled(value)
        _uiState.update { it.copy(simulatedNavAlertsEnabled = value) }
    }

    fun onHomeAddressDraftChange(value: String) {
        _uiState.update { it.copy(homeAddress = value) }
    }

    suspend fun saveHomeAddressFromDraft(): HomeAddressSaveResult {
        val normalized = HomeAddressInputRules.normalize(_uiState.value.homeAddress)
        if (normalized.isBlank()) return HomeAddressSaveResult.Empty
        if (!HomeAddressInputRules.isFormatValid(normalized)) return HomeAddressSaveResult.InvalidFormat

        when (homeAddressGeocodeVerifier.verify(normalized)) {
            HomeAddressGeocodeOutcome.NotFound -> return HomeAddressSaveResult.NotRecognizedOnMap
            HomeAddressGeocodeOutcome.Error -> return HomeAddressSaveResult.GeocodeError
            HomeAddressGeocodeOutcome.Resolved -> { /* continue */ }
        }

        return try {
            settingsInteractor.saveHomeAddress(normalized)
            _uiState.update { it.copy(homeAddress = normalized) }
            HomeAddressSaveResult.Saved
        } catch (_: Exception) {
            HomeAddressSaveResult.Failed
        }
    }

    fun saveHomeAddressFromDraftAsync(onResult: (HomeAddressSaveResult) -> Unit) {
        viewModelScope.launch {
            _homeAddressSaveInProgress.value = true
            try {
                onResult(saveHomeAddressFromDraft())
            } finally {
                _homeAddressSaveInProgress.value = false
            }
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
            privacyInteractor: PrivacyConsentInteractor,
            homeAddressGeocodeVerifier: HomeAddressGeocodeVerifier
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    settingsInteractor,
                    privacyInteractor,
                    homeAddressGeocodeVerifier
                ) as T
            }
        }

        fun defaultFactory(androidContext: android.content.Context): ViewModelProvider.Factory {
            val app = androidContext.applicationContext as? MyApplication
            val settings = app?.container?.settingsInteractor
                ?: SettingsInteractor(SettingsRepositoryImpl(androidContext.applicationContext))
            val privacy = app?.container?.privacyInteractor
                ?: PrivacyConsentInteractor(PrivacyRepositoryImpl(androidContext.applicationContext))
            val verifier = app?.container?.homeAddressGeocodeVerifier
                ?: BaiduHomeAddressGeocodeVerifier()
            return factory(settings, privacy, verifier)
        }
    }
}
