package com.example.earrove.data.privacy

import android.content.Context
import com.example.earrove.utils.PrivacyUtils

/**
 * 委托给既有 [PrivacyUtils] 实现，逐步将存储与 SDK 副作用收口到 data 层。
 */
class PrivacyRepositoryImpl(
    context: Context
) : PrivacyRepository {

    private val appContext = context.applicationContext

    override fun isFirstLaunch(): Boolean =
        PrivacyUtils.isFirstLaunch(appContext)

    override fun markAsNotFirstLaunch() {
        PrivacyUtils.markAsNotFirstLaunch(appContext)
    }

    override fun hasUserAgreedToAppPrivacy(): Boolean =
        PrivacyUtils.hasUserAgreedToAppPrivacy(appContext)

    override fun saveAppPrivacyAgreement(agreed: Boolean) {
        PrivacyUtils.saveAppPrivacyAgreement(appContext, agreed)
    }

    override fun hasUserAgreedToBaiduMapPrivacy(): Boolean =
        PrivacyUtils.hasUserAgreedToBaiduMapPrivacy(appContext)

    override fun saveBaiduMapPrivacyAgreement(agreed: Boolean) {
        PrivacyUtils.saveBaiduMapPrivacyAgreement(appContext, agreed)
    }

    override fun isBaiduSDKMarkedAsInitialized(): Boolean =
        PrivacyUtils.isBaiduSDKMarkedAsInitialized(appContext)

    override fun markBaiduSDKAsInitialized(initialized: Boolean) {
        PrivacyUtils.markBaiduSDKAsInitialized(appContext, initialized)
    }

    override fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean) {
        PrivacyUtils.saveAllPrivacyAgreements(appContext, appAgreed, baiduMapAgreed)
    }

    override fun areAllPrivacyAgreementsAccepted(): Boolean =
        PrivacyUtils.areAllPrivacyAgreementsAccepted(appContext)

    override fun clearAllPrivacyAgreements() {
        PrivacyUtils.clearAllPrivacyAgreements(appContext)
    }

    override fun isBaiduSDKSafeInitialized(): Boolean =
        PrivacyUtils.isBaiduSDKSafeInitialized()

    override fun retryBaiduSDKInitialization(
        onSuccess: (() -> Unit)?,
        onFailure: ((String) -> Unit)?
    ) {
        PrivacyUtils.retryBaiduSDKInitialization(appContext, onSuccess, onFailure)
    }

    override fun getPrivacyPolicyText(): String =
        PrivacyUtils.getPrivacyPolicyText()

    override fun getBaiduMapPrivacySummary(): String =
        PrivacyUtils.getBaiduMapPrivacySummary()
}
