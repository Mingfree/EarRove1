package com.example.earrove.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import androidx.compose.runtime.LaunchedEffect

/**
 * 隐私政策工具类
 * 负责管理百度地图SDK和其他第三方服务的隐私政策同意状态
 */
object PrivacyUtils {

    // SharedPreferences 键名
    private const val PREFS_NAME = "earrove_privacy_preferences"
    private const val KEY_BAIDU_MAP_PRIVACY_AGREED = "baidu_map_privacy_agreed"
    private const val KEY_APP_PRIVACY_AGREED = "app_privacy_agreed"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_BAIDU_SDK_INITIALIZED = "baidu_sdk_initialized"

    /**
     * 检查是否为首次启动应用
     */
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 标记应用已不是首次启动
     */
    fun markAsNotFirstLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * 检查用户是否已同意应用隐私政策
     */
    fun hasUserAgreedToAppPrivacy(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_PRIVACY_AGREED, false)
    }

    /**
     * 保存应用隐私政策同意状态
     */
    fun saveAppPrivacyAgreement(context: Context, agreed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_PRIVACY_AGREED, agreed).apply()
        Log.d("PrivacyUtils", "应用隐私政策同意状态已保存: $agreed")
    }

    /**
     * 检查用户是否已同意百度地图隐私政策
     */
    fun hasUserAgreedToBaiduMapPrivacy(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BAIDU_MAP_PRIVACY_AGREED, false)
    }

    /**
     * 检查百度地图SDK是否已标记为已初始化
     */
    fun isBaiduSDKMarkedAsInitialized(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BAIDU_SDK_INITIALIZED, false)
    }

    /**
     * 标记百度地图SDK为已初始化
     */
    fun markBaiduSDKAsInitialized(context: Context, initialized: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BAIDU_SDK_INITIALIZED, initialized).apply()
        Log.d("PrivacyUtils", "百度地图SDK初始化状态已保存: $initialized")
    }

    /**
     * 保存百度地图隐私政策同意状态并初始化SDK
     * 重要：必须使用 Application Context
     * 关键修复：添加延迟确保隐私政策设置生效
     */
    fun saveBaiduMapPrivacyAgreement(context: Context, agreed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BAIDU_MAP_PRIVACY_AGREED, agreed).apply()

        if (agreed) {
            try {
                val appContext = context.applicationContext

                Log.d("PrivacyUtils", "开始设置百度地图隐私政策同意")

                Handler(Looper.getMainLooper()).post {
                    try {
                        Log.d("PrivacyUtils", "调用 SDKInitializer.setAgreePrivacy")
                        SDKInitializer.setAgreePrivacy(appContext, true)
                        Log.d("PrivacyUtils", "百度地图隐私政策已设置同意")

                        // 延迟初始化SDK（确保隐私政策设置生效）
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (!SDKInitializer.isInitialized()) {
                                    Log.d("PrivacyUtils", "开始初始化百度地图SDK...")
                                    SDKInitializer.initialize(appContext)
                                    SDKInitializer.setCoordType(CoordType.BD09LL)

                                    markBaiduSDKAsInitialized(appContext, true)
                                    Log.d("PrivacyUtils", "百度地图SDK初始化成功")
                                } else {
                                    Log.d("PrivacyUtils", "百度地图SDK已经初始化")
                                    markBaiduSDKAsInitialized(appContext, true)
                                }
                            } catch (e: Exception) {
                                Log.e("PrivacyUtils", "百度地图SDK初始化失败: ${e.message}", e)
                                markBaiduSDKAsInitialized(appContext, false)
                            }
                        }, 500) // 延迟500ms确保隐私政策设置生效

                    } catch (e: Exception) {
                        Log.e("PrivacyUtils", "设置百度地图隐私政策失败: ${e.message}", e)
                        markBaiduSDKAsInitialized(appContext, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("PrivacyUtils", "设置百度地图隐私政策失败: ${e.message}", e)
                markBaiduSDKAsInitialized(context.applicationContext, false)
            }
        } else {
            Log.d("PrivacyUtils", "用户未同意百度地图隐私政策")
            markBaiduSDKAsInitialized(context.applicationContext, false)
        }
    }

    /**
     * 一次性保存所有隐私政策同意状态
     */
    fun saveAllPrivacyAgreements(context: Context, appAgreed: Boolean, baiduMapAgreed: Boolean) {
        saveAppPrivacyAgreement(context, appAgreed)
        saveBaiduMapPrivacyAgreement(context, baiduMapAgreed)

        if (appAgreed && baiduMapAgreed) {
            Log.d("PrivacyUtils", "用户已同意所有隐私政策")
        }
    }

    /**
     * 检查所有必需的隐私政策是否都已同意
     */
    fun areAllPrivacyAgreementsAccepted(context: Context): Boolean {
        return hasUserAgreedToAppPrivacy(context) &&
                hasUserAgreedToBaiduMapPrivacy(context) &&
                isBaiduSDKMarkedAsInitialized(context)
    }

    /**
     * 清除所有隐私政策同意状态（用于测试或退出登录）
     */
    fun clearAllPrivacyAgreements(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_APP_PRIVACY_AGREED)
            .remove(KEY_BAIDU_MAP_PRIVACY_AGREED)
            .remove(KEY_BAIDU_SDK_INITIALIZED)
            .apply()
        Log.d("PrivacyUtils", "所有隐私政策同意状态已清除")
    }

    /**
     * 获取隐私政策的详细描述
     */
    fun getPrivacyPolicyText(): String {
        return """
        隐私政策声明
        
        1. 信息收集
        - 位置信息：用于提供导航服务，不会上传服务器
        - 语音输入：用于接收语音指令，本地处理不上传
        - 摄像头数据：仅用于文字识别，内存处理不保存
        
        2. 数据使用
        - 所有数据仅在设备本地处理
        - 不收集个人身份信息
        - 不与第三方分享用户数据
        
        3. 第三方服务
        - 百度地图SDK：用于导航功能
        - Google ML Kit：用于本地文字识别
        
        4. 用户权利
        - 随时可以撤回同意
        - 可以清除本地存储的数据
        - 可以卸载应用停止所有数据收集
        
        5. 联系我们
        - 如有隐私相关问题，请联系：privacy@earrove.com
        
        更新日期：2024年1月
        """.trimIndent()
    }

    /**
     * 获取百度地图隐私政策摘要
     */
    fun getBaiduMapPrivacySummary(): String {
        return """
        百度地图SDK隐私政策摘要：
        
        1. 百度地图SDK会收集设备信息、位置信息用于地图服务
        2. 这些信息用于提供定位、导航等核心功能
        3. 百度地图遵守相关法律法规保护用户隐私
        4. 详细政策请参考百度地图官方隐私政策
        
        注：本应用已配置百度地图SDK仅使用必要的最小权限。
        """.trimIndent()
    }

    /**
     * 安全地检查SDK是否已初始化
     */
    fun isBaiduSDKSafeInitialized(): Boolean {
        return try {
            SDKInitializer.isInitialized()
        } catch (e: Exception) {
            Log.e("PrivacyUtils", "检查SDK初始化状态失败: ${e.message}")
            false
        }
    }

    /**
     * 尝试重新初始化SDK（当检测到未初始化时调用）
     */
    fun retryBaiduSDKInitialization(context: Context, onSuccess: (() -> Unit)? = null, onFailure: ((String) -> Unit)? = null) {
        if (!hasUserAgreedToBaiduMapPrivacy(context)) {
            onFailure?.invoke("用户未同意百度地图隐私政策")
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                val appContext = context.applicationContext

                Log.d("PrivacyUtils", "开始重新初始化SDK，设置隐私政策")
                SDKInitializer.setAgreePrivacy(appContext, true)

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (!SDKInitializer.isInitialized()) {
                            Log.d("PrivacyUtils", "SDK未初始化，开始初始化")
                            SDKInitializer.initialize(appContext)
                            SDKInitializer.setCoordType(CoordType.BD09LL)
                            markBaiduSDKAsInitialized(appContext, true)
                            Log.d("PrivacyUtils", "百度地图SDK重新初始化成功")
                            onSuccess?.invoke()
                        } else {
                            markBaiduSDKAsInitialized(appContext, true)
                            Log.d("PrivacyUtils", "百度地图SDK已经初始化")
                            onSuccess?.invoke()
                        }
                    } catch (e: Exception) {
                        Log.e("PrivacyUtils", "百度地图SDK重新初始化失败: ${e.message}")
                        markBaiduSDKAsInitialized(appContext, false)
                        onFailure?.invoke(e.message ?: "未知错误")
                    }
                }, 300)
            } catch (e: Exception) {
                Log.e("PrivacyUtils", "重新设置隐私政策失败: ${e.message}")
                onFailure?.invoke(e.message ?: "未知错误")
            }
        }
    }

    /**
     * Composable函数：检查并请求隐私政策同意
     * @param onAllAgreed 当所有隐私政策都同意时的回调
     * @param onShowDialog 需要显示隐私政策对话框时的回调
     */
    @Composable
    fun CheckPrivacyAgreements(
        onAllAgreed: () -> Unit,
        onShowDialog: () -> Unit
    ) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            if (areAllPrivacyAgreementsAccepted(context)) {
                // 所有隐私政策都已同意
                onAllAgreed()
            } else {
                // 需要显示隐私政策对话框
                onShowDialog()
            }
        }
    }

    /**
     * Composable函数：检查百度地图SDK初始化状态
     * @param onInitialized SDK已初始化的回调
     * @param onNotInitialized SDK未初始化的回调
     */
    @Composable
    fun CheckBaiduSDKInitialization(
        onInitialized: () -> Unit,
        onNotInitialized: () -> Unit
    ) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            // 检查存储的初始化状态
            val storedInitialized = isBaiduSDKMarkedAsInitialized(context)

            if (storedInitialized) {
                // 如果标记为已初始化，再检查实际状态
                val actualInitialized = isBaiduSDKSafeInitialized()
                if (actualInitialized) {
                    onInitialized()
                } else {
                    // 存储状态与实际状态不一致，尝试重新初始化
                    retryBaiduSDKInitialization(context,
                        onSuccess = { onInitialized() },
                        onFailure = { error ->
                            Log.e("PrivacyUtils", "重新初始化失败: $error")
                            onNotInitialized()
                        }
                    )
                }
            } else {
                onNotInitialized()
            }
        }
    }
}

/**
 * 隐私政策同意状态数据类
 */
data class PrivacyAgreementStatus(
    val appPrivacyAgreed: Boolean = false,
    val baiduMapPrivacyAgreed: Boolean = false,
    val baiduSDKInitialized: Boolean = false,
    val firstLaunch: Boolean = true,
    val lastAgreementTime: Long = 0
) {
    val allAgreed: Boolean
        get() = appPrivacyAgreed && baiduMapPrivacyAgreed && baiduSDKInitialized
}