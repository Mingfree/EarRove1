package com.example.earrove.ui.settings

import com.example.earrove.data.privacy.PrivacyRepository
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private class FakeSettingsInteractor : SettingsInteractor(
        object : SettingsRepository {
            private var rate = 1f
            private var haptic = true
            private var visual = false
            private var home = ""
            override fun getTtsSpeechRate() = rate
            override fun setTtsSpeechRate(value: Float) {
                rate = value
            }

            override fun isHapticEnabled() = haptic
            override fun setHapticEnabled(value: Boolean) {
                haptic = value
            }

            override fun isVisualAssistEnabled() = visual
            override fun setVisualAssistEnabled(value: Boolean) {
                visual = value
            }

            override fun getHomeAddress(): String? = home.ifBlank { null }
            override fun setHomeAddress(value: String) {
                home = value
            }

            override fun clearHomeAddress() {
                home = ""
            }
        }
    )

    private class FakePrivacyInteractor : PrivacyConsentInteractor(
        object : PrivacyRepository {
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
    )

    @Test
    fun onTtsSpeechRateChange_updatesState() {
        val vm = SettingsViewModel(FakeSettingsInteractor(), FakePrivacyInteractor())
        vm.onTtsSpeechRateChange(1.5f)
        assertEquals(1.5f, vm.uiState.value.ttsSpeechRate, 0.001f)
    }

    @Test
    fun initialState_matchesInteractorSnapshot() {
        val settings = FakeSettingsInteractor()
        val vm = SettingsViewModel(settings, FakePrivacyInteractor())
        assertEquals(settings.loadSnapshot(), vm.uiState.value)
    }
}
