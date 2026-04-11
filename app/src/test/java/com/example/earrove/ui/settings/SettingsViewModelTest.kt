package com.example.earrove.ui.settings

import com.example.earrove.data.privacy.PrivacyRepository
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private fun fakeSettingsInteractor(): SettingsInteractor {
        val repo = object : SettingsRepository {
            private var rate = 1f
            private var haptic = true
            private var home = ""
            override fun getTtsSpeechRate() = rate
            override fun setTtsSpeechRate(value: Float) {
                rate = value
            }

            override fun isHapticEnabled() = haptic
            override fun setHapticEnabled(value: Boolean) {
                haptic = value
            }

            override fun getHomeAddress(): String? = home.ifBlank { null }
            override fun setHomeAddress(value: String) {
                home = value
            }

            override fun clearHomeAddress() {
                home = ""
            }
        }
        return SettingsInteractor(repo)
    }

    private fun fakePrivacyInteractor(): PrivacyConsentInteractor {
        val repo = object : PrivacyRepository {
            override fun isFirstLaunch() = false
            override fun markAsNotFirstLaunch() {}
            override fun hasUserAgreedToAppPrivacy() = true
            override fun saveAppPrivacyAgreement(agreed: Boolean) {}
            override fun hasUserAgreedToBaiduMapPrivacy() = true
            override fun saveBaiduMapPrivacyAgreement(agreed: Boolean) {}
            override fun isBaiduSDKMarkedAsInitialized() = true
            override fun markBaiduSDKAsInitialized(initialized: Boolean) {}
            override fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean) {}
            override fun areAllPrivacyAgreementsAccepted() = true
            override fun clearAllPrivacyAgreements() {}
            override fun isBaiduSDKSafeInitialized() = true
            override fun retryBaiduSDKInitialization(
                onSuccess: (() -> Unit)?,
                onFailure: ((String) -> Unit)?
            ) {
            }

            override fun getPrivacyPolicyText() = ""
            override fun getBaiduMapPrivacySummary() = ""
        }
        return PrivacyConsentInteractor(repo)
    }

    @Test
    fun onTtsSpeechRateChange_updatesState() {
        val vm = SettingsViewModel(fakeSettingsInteractor(), fakePrivacyInteractor())
        vm.onTtsSpeechRateChange(1.5f)
        assertEquals(1.5f, vm.uiState.value.ttsSpeechRate, 0.001f)
    }

    @Test
    fun initialState_matchesInteractorSnapshot() {
        val settingsInteractor = fakeSettingsInteractor()
        val vm = SettingsViewModel(settingsInteractor, fakePrivacyInteractor())
        assertEquals(settingsInteractor.loadSnapshot(), vm.uiState.value)
    }
}
