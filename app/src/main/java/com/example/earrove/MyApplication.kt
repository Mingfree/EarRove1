package com.example.earrove

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.common.BaiduMapSDKException
import com.example.earrove.data.privacy.PrivacyRepositoryImpl

class MyApplication : Application() {

    private val privacyRepository by lazy { PrivacyRepositoryImpl(this) }

    companion object {
        // 单例实例
        private lateinit var instance: MyApplication

        @JvmStatic
        fun getInstance(): MyApplication {
            return instance
        }

        // SDK初始化状态
        private var sdkInitializationAttempted = false
        private var sdkInitializationSuccess = false
    }

    override fun onCreate() {
        super.onCreate()

        // 设置实例
        instance = this

        Log.d("MyApplication", "MyApplication onCreate开始")

        // 检查是否首次启动
        if (privacyRepository.isFirstLaunch()) {
            Log.d("MyApplication", "首次启动应用")
            privacyRepository.markAsNotFirstLaunch()
        } else {
            Log.d("MyApplication", "非首次启动应用")
        }

        // 检查用户是否已同意隐私政策
        if (privacyRepository.hasUserAgreedToBaiduMapPrivacy()) {
            // 用户已同意，初始化百度地图SDK（在主线程）
            initializeBaiduMapSDKOnAppStart()
        } else {
            Log.d("MyApplication", "用户尚未同意百度地图隐私政策，等待用户同意")
        }

        // 初始化其他安全组件
        initializeSafeComponents()

        Log.d("MyApplication", "MyApplication onCreate完成")
    }

    /**
     * 应用启动时初始化百度地图SDK
     */
    private fun initializeBaiduMapSDKOnAppStart() {
        Log.d("MyApplication", "应用启动：开始初始化百度地图SDK")

        // 使用PrivacyUtils的增强初始化方法
        if (!sdkInitializationAttempted) {
            sdkInitializationAttempted = true

            // 检查SDK是否已经通过PrivacyUtils初始化
            val storedInitialized = privacyRepository.isBaiduSDKMarkedAsInitialized()

            if (storedInitialized) {
                // 如果已经标记为初始化，检查实际状态
                val actualInitialized = privacyRepository.isBaiduSDKSafeInitialized()
                if (actualInitialized) {
                    sdkInitializationSuccess = true
                    Log.d("MyApplication", "SDK已通过PrivacyUtils成功初始化")
                } else {
                    // 状态不一致，尝试重新初始化
                    Log.w("MyApplication", "SDK状态不一致，尝试重新初始化")
                    retryBaiduSDKInitialization()
                }
            } else {
                // 尚未初始化，使用新的安全方法
                retryBaiduSDKInitialization()
            }
        } else {
            Log.d("MyApplication", "SDK初始化已尝试过，跳过")
        }
    }

    /**
     * 使用PrivacyUtils的安全方法重新初始化SDK
     */
    private fun retryBaiduSDKInitialization() {
        Log.d("MyApplication", "开始重新初始化百度地图SDK...")

        privacyRepository.retryBaiduSDKInitialization(
            onSuccess = {
                sdkInitializationSuccess = true
                Log.d("MyApplication", "百度地图SDK重新初始化成功")
            },
            onFailure = { error ->
                sdkInitializationSuccess = false
                Log.e("MyApplication", "百度地图SDK重新初始化失败: $error")
                // 可以在这里添加重试逻辑或通知用户
            }
        )
    }

    /**
     * 公开方法：初始化百度地图SDK（当用户同意隐私政策后调用）
     * 现在调用PrivacyUtils的增强方法
     */
    fun initializeBaiduMapSDK() {
        Log.d("MyApplication", "收到请求初始化百度地图SDK")

        if (!privacyRepository.hasUserAgreedToBaiduMapPrivacy()) {
            Log.w("MyApplication", "用户未同意百度地图隐私政策，无法初始化SDK")
            return
        }

        // 标记为尝试初始化
        sdkInitializationAttempted = true

        // 使用PrivacyUtils的安全初始化方法
        privacyRepository.retryBaiduSDKInitialization(
            onSuccess = {
                sdkInitializationSuccess = true
                Log.d("MyApplication", "百度地图SDK初始化成功")
            },
            onFailure = { error ->
                sdkInitializationSuccess = false
                Log.e("MyApplication", "百度地图SDK初始化失败: $error")
            }
        )
    }

    /**
     * 检查百度地图SDK是否已成功初始化
     * 现在结合多个状态检查
     */
    fun isBaiduMapSDKInitialized(): Boolean {
        return try {
            // 检查三个状态的一致性
            val privacyAgreed = privacyRepository.hasUserAgreedToBaiduMapPrivacy()
            val markedInitialized = privacyRepository.isBaiduSDKMarkedAsInitialized()
            val actualInitialized = privacyRepository.isBaiduSDKSafeInitialized()
            val appStateInitialized = sdkInitializationSuccess

            Log.d("MyApplication", "SDK状态检查: " +
                    "隐私同意=$privacyAgreed, " +
                    "标记初始化=$markedInitialized, " +
                    "实际初始化=$actualInitialized, " +
                    "应用状态=$appStateInitialized")

            // 所有状态都应该一致
            val isInitialized = privacyAgreed &&
                    markedInitialized &&
                    actualInitialized &&
                    appStateInitialized

            if (!isInitialized && privacyAgreed) {
                // 如果隐私已同意但状态不一致，尝试修复
                Log.w("MyApplication", "SDK状态不一致，尝试修复...")
                if (markedInitialized != actualInitialized) {
                    privacyRepository.markBaiduSDKAsInitialized(actualInitialized)
                }

                if (!actualInitialized && !sdkInitializationAttempted) {
                    // 如果实际未初始化且未尝试过，尝试初始化
                    Handler(Looper.getMainLooper()).postDelayed({
                        initializeBaiduMapSDK()
                    }, 1000)
                }
            }

            isInitialized
        } catch (e: Exception) {
            Log.e("MyApplication", "检查SDK初始化状态异常: ${e.message}")
            false
        }
    }

    /**
     * 获取SDK初始化状态的详细报告
     */
    fun getBaiduSDKStatusReport(): Map<String, Any> {
        return mapOf(
            "privacyAgreed" to privacyRepository.hasUserAgreedToBaiduMapPrivacy(),
            "markedInitialized" to privacyRepository.isBaiduSDKMarkedAsInitialized(),
            "actualInitialized" to privacyRepository.isBaiduSDKSafeInitialized(),
            "appAttempted" to sdkInitializationAttempted,
            "appSuccess" to sdkInitializationSuccess,
            "sdkInitialized" to try { SDKInitializer.isInitialized() } catch (e: Exception) { false }
        )
    }

    /**
     * 强制重新初始化SDK（用于调试或恢复）
     */
    fun forceReinitializeBaiduSDK(onComplete: (success: Boolean, error: String?) -> Unit) {
        Log.d("MyApplication", "强制重新初始化百度地图SDK")

        // 重置状态
        sdkInitializationAttempted = false
        sdkInitializationSuccess = false
        privacyRepository.markBaiduSDKAsInitialized(false)

        // 使用PrivacyUtils的安全方法
        privacyRepository.retryBaiduSDKInitialization(
            onSuccess = {
                sdkInitializationSuccess = true
                onComplete(true, null)
            },
            onFailure = { error ->
                sdkInitializationSuccess = false
                onComplete(false, error)
            }
        )
    }

    /**
     * 初始化其他安全组件（不需要隐私同意的组件）
     */
    private fun initializeSafeComponents() {
        Log.d("MyApplication", "初始化其他安全组件...")
        // 例如：数据库、网络库、崩溃报告等
        // 但不包括需要用户隐私同意的百度地图SDK

        // 初始化协程（如果使用）
        // 初始化数据库（如果使用Room）
        // 初始化网络客户端（如果使用Retrofit）
        // 初始化崩溃报告（如Firebase Crashlytics）
    }

    init {
        Log.d("MyApplication", "MyApplication构造函数执行")
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("MyApplication", "MyApplication终止")
        // 可以在这里清理资源
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("MyApplication", "MyApplication收到低内存警告")
        // 可以在这里释放非关键资源
    }
}