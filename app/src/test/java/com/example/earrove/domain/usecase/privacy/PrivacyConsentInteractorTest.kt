package com.example.earrove.domain.usecase.privacy

import com.example.earrove.data.privacy.PrivacyRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyConsentInteractorTest {

    private class FakePrivacyRepository : PrivacyRepository {
        var cleared = 0
        var appAgreed = false
        var baiduAgreed = false

        override fun isFirstLaunch() = false
        override fun markAsNotFirstLaunch() {}

        override fun hasUserAgreedToAppPrivacy() = appAgreed
        override fun saveAppPrivacyAgreement(agreed: Boolean) {
            appAgreed = agreed
        }

        override fun hasUserAgreedToBaiduMapPrivacy() = baiduAgreed
        override fun saveBaiduMapPrivacyAgreement(agreed: Boolean) {
            baiduAgreed = agreed
        }

        override fun isBaiduSDKMarkedAsInitialized() = false
        override fun markBaiduSDKAsInitialized(initialized: Boolean) {}

        override fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean) {
            this.appAgreed = appAgreed
            this.baiduAgreed = baiduMapAgreed
        }

        override fun areAllPrivacyAgreementsAccepted() = appAgreed && baiduAgreed

        override fun clearAllPrivacyAgreements() {
            cleared++
            appAgreed = false
            baiduAgreed = false
        }

        override fun isBaiduSDKSafeInitialized() = false
        override fun retryBaiduSDKInitialization(
            onSuccess: (() -> Unit)?,
            onFailure: ((String) -> Unit)?
        ) {
        }

        override fun getPrivacyPolicyText() = "policy"
        override fun getBaiduMapPrivacySummary() = "baidu"
    }

    @Test
    fun revoke_delegatesToRepositoryClear() {
        val repo = FakePrivacyRepository().apply {
            appAgreed = true
            baiduAgreed = true
        }
        val sut = PrivacyConsentInteractor(repo)

        sut.revokeAllPrivacyAgreements()

        assertEquals(1, repo.cleared)
        assertEquals(false, repo.hasUserAgreedToAppPrivacy())
        assertEquals(false, repo.hasUserAgreedToBaiduMapPrivacy())
    }
}

