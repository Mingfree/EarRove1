package com.example.earrove.data.privacy

/**
 * 隐私同意状态与百度 SDK 相关流程的抽象；具体持久化与 SDK 调用由实现类完成。
 */
interface PrivacyRepository {
    fun isFirstLaunch(): Boolean
    fun markAsNotFirstLaunch()

    fun hasUserAgreedToAppPrivacy(): Boolean
    fun saveAppPrivacyAgreement(agreed: Boolean)

    fun hasUserAgreedToBaiduMapPrivacy(): Boolean
    fun saveBaiduMapPrivacyAgreement(agreed: Boolean)

    fun isBaiduSDKMarkedAsInitialized(): Boolean
    fun markBaiduSDKAsInitialized(initialized: Boolean)

    fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean)
    fun areAllPrivacyAgreementsAccepted(): Boolean
    fun clearAllPrivacyAgreements()

    fun isBaiduSDKSafeInitialized(): Boolean
    fun retryBaiduSDKInitialization(
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    )

    fun getPrivacyPolicyText(): String
    fun getBaiduMapPrivacySummary(): String
}
