package com.example.earrove.ui.settings

import com.example.earrove.data.privacy.PrivacyRepository
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import com.example.earrove.domain.validation.HomeAddressGeocodeOutcome
import com.example.earrove.domain.validation.HomeAddressGeocodeVerifier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private fun fakeSettingsInteractor(): SettingsInteractor {
        val repo = object : SettingsRepository {
            private var rate = 1f
            private var haptic = true
            private var simulatedNavAlerts = false
            private var home = ""
            override fun getTtsSpeechRate() = rate
            override fun setTtsSpeechRate(value: Float) {
                rate = value
            }

            override fun isHapticEnabled() = haptic
            override fun setHapticEnabled(value: Boolean) {
                haptic = value
            }

            override fun isSimulatedNavAlertsEnabled() = simulatedNavAlerts
            override fun setSimulatedNavAlertsEnabled(value: Boolean) {
                simulatedNavAlerts = value
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

    private fun verifierFixed(outcome: HomeAddressGeocodeOutcome) =
        HomeAddressGeocodeVerifier { outcome }

    @Test
    fun onTtsSpeechRateChange_updatesState() {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onTtsSpeechRateChange(1.5f)
        assertEquals(1.5f, vm.uiState.value.ttsSpeechRate, 0.001f)
    }

    @Test
    fun onSimulatedNavAlertsChange_updatesState() {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        assertEquals(false, vm.uiState.value.simulatedNavAlertsEnabled)
        vm.onSimulatedNavAlertsChange(true)
        assertEquals(true, vm.uiState.value.simulatedNavAlertsEnabled)
    }

    @Test
    fun initialState_matchesInteractorSnapshot() {
        val settingsInteractor = fakeSettingsInteractor()
        val vm = SettingsViewModel(
            settingsInteractor,
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        assertEquals(settingsInteractor.loadSnapshot(), vm.uiState.value)
    }

    @Test
    fun saveHomeAddressFromDraft_blank_returnsEmpty() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onHomeAddressDraftChange("   ")
        assertEquals(HomeAddressSaveResult.Empty, vm.saveHomeAddressFromDraft())
    }

    @Test
    fun saveHomeAddressFromDraft_noIdeograph_returnsInvalidFormat() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onHomeAddressDraftChange("1234 Main St")
        assertEquals(HomeAddressSaveResult.InvalidFormat, vm.saveHomeAddressFromDraft())
    }

    @Test
    fun saveHomeAddressFromDraft_notFound_returnsNotRecognizedOnMap() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.NotFound)
        )
        vm.onHomeAddressDraftChange("北京市海淀区某某路某某号")
        assertEquals(HomeAddressSaveResult.NotRecognizedOnMap, vm.saveHomeAddressFromDraft())
    }

    @Test
    fun saveHomeAddressFromDraft_geocodeError_returnsGeocodeError() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Error)
        )
        vm.onHomeAddressDraftChange("北京市朝阳区三里屯")
        assertEquals(HomeAddressSaveResult.GeocodeError, vm.saveHomeAddressFromDraft())
    }

    @Test
    fun saveHomeAddressFromDraft_valid_returnsSaved_andPersists() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onHomeAddressDraftChange("  北京市测试路1号  ")
        assertEquals(HomeAddressSaveResult.Saved, vm.saveHomeAddressFromDraft())
        assertEquals("北京市测试路1号", vm.uiState.value.homeAddress)
    }

    @Test
    fun clearHomeAddress_whenEmpty_returnsNothingToClear() {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        assertEquals(HomeAddressClearResult.NothingToClear, vm.clearHomeAddress())
    }

    @Test
    fun clearHomeAddress_whenDraftOnly_returnsCleared() {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onHomeAddressDraftChange("  草稿  ")
        assertEquals(HomeAddressClearResult.Cleared, vm.clearHomeAddress())
        assertEquals("", vm.uiState.value.homeAddress)
    }

    @Test
    fun clearHomeAddress_whenSaved_returnsCleared() = runTest {
        val vm = SettingsViewModel(
            fakeSettingsInteractor(),
            fakePrivacyInteractor(),
            verifierFixed(HomeAddressGeocodeOutcome.Resolved)
        )
        vm.onHomeAddressDraftChange("北京市朝阳区")
        vm.saveHomeAddressFromDraft()
        assertEquals(HomeAddressClearResult.Cleared, vm.clearHomeAddress())
        assertEquals("", vm.uiState.value.homeAddress)
    }
}
