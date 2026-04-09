package com.example.earrove.domain.usecase.privacy

import com.example.earrove.data.privacy.PrivacyRepository

/**
 * 隐私同意流程的用例封装（启动页、设置撤回等）。
 */
class PrivacyConsentInteractor(
    private val privacyRepository: PrivacyRepository
) {
    fun hasAgreedToAppPrivacy(): Boolean =
        privacyRepository.hasUserAgreedToAppPrivacy()

    fun hasAgreedToBaiduMapPrivacy(): Boolean =
        privacyRepository.hasUserAgreedToBaiduMapPrivacy()

    fun saveAppPrivacyAgreement(agreed: Boolean) {
        privacyRepository.saveAppPrivacyAgreement(agreed)
    }

    fun saveBaiduMapPrivacyAgreement(agreed: Boolean) {
        privacyRepository.saveBaiduMapPrivacyAgreement(agreed)
    }

    fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean) {
        privacyRepository.saveAllPrivacyAgreements(appAgreed, baiduMapAgreed)
    }

    fun revokeAllPrivacyAgreements() {
        privacyRepository.clearAllPrivacyAgreements()
    }

    fun getPrivacyPolicyText(): String = privacyRepository.getPrivacyPolicyText()
    fun getBaiduMapPrivacySummary(): String = privacyRepository.getBaiduMapPrivacySummary()
}
