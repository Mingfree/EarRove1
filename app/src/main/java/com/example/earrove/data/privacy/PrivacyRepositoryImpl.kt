package com.example.earrove.data.privacy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.earrove.utils.PrivacyUtils

/**
 * 隐私同意状态的持久化与百度地图 SDK 生命周期编排，收口在 data 层。
 *
 * SharedPreferences 文件与 key 保持既有存储契约不变——androidTest 中
 * `SmokeNavigationAndSettingsTest` 依赖固定的文件名与 key 名预置同意状态，
 * 因此本次只下沉「读写位置」，不改「存储契约」。
 */
class PrivacyRepositoryImpl(
    context: Context
) : PrivacyRepository {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isFirstLaunch(): Boolean =
        prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    override fun markAsNotFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    override fun hasUserAgreedToAppPrivacy(): Boolean =
        prefs.getBoolean(KEY_APP_PRIVACY_AGREED, false)

    override fun saveAppPrivacyAgreement(agreed: Boolean) {
        prefs.edit().putBoolean(KEY_APP_PRIVACY_AGREED, agreed).apply()
        Log.d(TAG, "应用隐私政策同意状态已保存: $agreed")
    }

    override fun hasUserAgreedToBaiduMapPrivacy(): Boolean =
        prefs.getBoolean(KEY_BAIDU_MAP_PRIVACY_AGREED, false)

    override fun saveBaiduMapPrivacyAgreement(agreed: Boolean) {
        prefs.edit().putBoolean(KEY_BAIDU_MAP_PRIVACY_AGREED, agreed).apply()

        if (agreed) {
            PrivacyUtils.initializeBaiduSdk(
                context = appContext,
                onSuccess = {
                    markBaiduSDKAsInitialized(true)
                    Log.d(TAG, "百度地图SDK初始化成功")
                },
                onFailure = { error ->
                    markBaiduSDKAsInitialized(false)
                    Log.e(TAG, "百度地图SDK初始化失败: $error")
                }
            )
        } else {
            Log.d(TAG, "用户未同意百度地图隐私政策")
            markBaiduSDKAsInitialized(false)
        }
    }

    override fun isBaiduSDKMarkedAsInitialized(): Boolean =
        prefs.getBoolean(KEY_BAIDU_SDK_INITIALIZED, false)

    override fun markBaiduSDKAsInitialized(initialized: Boolean) {
        prefs.edit().putBoolean(KEY_BAIDU_SDK_INITIALIZED, initialized).apply()
        Log.d(TAG, "百度地图SDK初始化状态已保存: $initialized")
    }

    override fun saveAllPrivacyAgreements(appAgreed: Boolean, baiduMapAgreed: Boolean) {
        saveAppPrivacyAgreement(appAgreed)
        saveBaiduMapPrivacyAgreement(baiduMapAgreed)

        if (appAgreed && baiduMapAgreed) {
            Log.d(TAG, "用户已同意所有隐私政策")
        }
    }

    override fun areAllPrivacyAgreementsAccepted(): Boolean =
        hasUserAgreedToAppPrivacy() &&
            hasUserAgreedToBaiduMapPrivacy() &&
            isBaiduSDKMarkedAsInitialized()

    override fun clearAllPrivacyAgreements() {
        prefs.edit()
            .remove(KEY_APP_PRIVACY_AGREED)
            .remove(KEY_BAIDU_MAP_PRIVACY_AGREED)
            .remove(KEY_BAIDU_SDK_INITIALIZED)
            .remove(KEY_FIRST_LAUNCH)
            .apply()
        Log.d(TAG, "所有隐私政策同意状态已清除")
    }

    override fun isBaiduSDKSafeInitialized(): Boolean =
        PrivacyUtils.isBaiduSDKSafeInitialized()

    override fun retryBaiduSDKInitialization(
        onSuccess: (() -> Unit)?,
        onFailure: ((String) -> Unit)?
    ) {
        if (!hasUserAgreedToBaiduMapPrivacy()) {
            onFailure?.invoke("用户未同意百度地图隐私政策")
            return
        }

        PrivacyUtils.initializeBaiduSdk(
            context = appContext,
            onSuccess = {
                markBaiduSDKAsInitialized(true)
                onSuccess?.invoke()
            },
            onFailure = { error ->
                markBaiduSDKAsInitialized(false)
                onFailure?.invoke(error)
            }
        )
    }

    override fun getPrivacyPolicyText(): String =
        PrivacyUtils.getPrivacyPolicyText()

    override fun getBaiduMapPrivacySummary(): String =
        PrivacyUtils.getBaiduMapPrivacySummary()

    private companion object {
        const val TAG = "PrivacyRepository"

        // 存储契约（文件名与 key 名）保持不变，androidTest 预置同意状态依赖这些值。
        const val PREFS_NAME = "earrove_privacy_preferences"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_APP_PRIVACY_AGREED = "app_privacy_agreed"
        const val KEY_BAIDU_MAP_PRIVACY_AGREED = "baidu_map_privacy_agreed"
        const val KEY_BAIDU_SDK_INITIALIZED = "baidu_sdk_initialized"
    }
}
