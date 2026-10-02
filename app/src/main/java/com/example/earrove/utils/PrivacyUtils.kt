package com.example.earrove.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer

/**
 * 隐私政策文本与百度地图 SDK 生命周期工具类。
 *
 * 隐私同意状态的持久化（SharedPreferences 文件与 key）已下沉到
 * `data/privacy/PrivacyRepositoryImpl`，本类只保留与存储无关的职责：
 * 政策文本、SDK 初始化与状态检查。
 */
object PrivacyUtils {

    /**
     * 获取隐私政策的详细描述
     */
    fun getPrivacyPolicyText(): String = """
        隐私政策声明

        1. 信息收集与使用
        - 位置信息：用于提供定位、导航与周边地点检索服务，会发送至百度地图SDK
        - 语音输入：用于识别您说出的目的地，录制的音频会经网络发送至第三方语音识别服务
        - 摄像头与相册图片：用于文字识别，图片会经网络发送至第三方图像识别服务
        - 识别与解析结果仅用于本机播报与显示，不在本机长期保存图片或原始音频

        2. 数据使用
        - 除上述导航、语音识别、文字识别与地点解析外，不将数据用于其他用途
        - 不收集姓名、身份证号等个人身份信息
        - 数据仅提供给我们选用的第三方服务商处理，不会出售或提供给其他无关方

        3. 第三方服务
        - 地图服务商：用于定位、导航与周边地点检索
        - 语音合成服务商：用于语音播报
        - 语音识别服务商：用于识别您说出的目的地，需上传录制的音频
        - 大模型服务商：用于从识别文本中解析目的地地名
        - 图像识别服务商：用于文字识别，需上传待识别图片
        - 上述第三方依照其各自隐私政策处理数据，建议您一并阅读

        4. 用户权利
        - 随时可以撤回同意
        - 可以清除本机保存的同意状态、设置等数据
        - 可以卸载应用停止所有数据收集

        5. 联系我们
        - 如有隐私相关问题，请联系：18346448989@163.com

        更新日期：2026年9月
    """.trimIndent()

    /**
     * 获取百度地图隐私政策摘要
     */
    fun getBaiduMapPrivacySummary(): String = """
        百度地图SDK隐私政策摘要：

        1. 百度地图SDK会收集设备信息、位置信息用于地图服务
        2. 这些信息用于提供定位、导航等核心功能
        3. 百度地图遵守相关法律法规保护用户隐私
        4. 详细政策请参考百度地图官方隐私政策

        注：本应用已配置百度地图SDK仅使用必要的最小权限。
    """.trimIndent()

    /**
     * 安全地检查SDK是否已初始化
     */
    fun isBaiduSDKSafeInitialized(): Boolean = try {
        SDKInitializer.isInitialized()
    } catch (e: Exception) {
        Log.e("PrivacyUtils", "检查SDK初始化状态失败: ${e.message}")
        false
    }

    /**
     * 设置百度地图隐私政策并初始化 SDK（在主线程延迟执行，确保隐私设置生效）。
     * 仅负责 SDK 生命周期，不直接读写偏好设置；初始化结果经回调返回，
     * 由调用方（repository）决定如何持久化「已初始化」标记。
     */
    fun initializeBaiduSdk(context: Context, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val appContext = context.applicationContext

        Handler(Looper.getMainLooper()).post {
            try {
                Log.d("PrivacyUtils", "开始设置百度地图隐私政策同意")
                SDKInitializer.setAgreePrivacy(appContext, true)
                Log.d("PrivacyUtils", "百度地图隐私政策已设置同意")

                // 延迟初始化SDK（确保隐私政策设置生效）
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (!SDKInitializer.isInitialized()) {
                            Log.d("PrivacyUtils", "开始初始化百度地图SDK...")
                            SDKInitializer.initialize(appContext)
                            SDKInitializer.setCoordType(CoordType.BD09LL)
                            Log.d("PrivacyUtils", "百度地图SDK初始化成功")
                        } else {
                            Log.d("PrivacyUtils", "百度地图SDK已经初始化")
                        }
                        onSuccess()
                    } catch (e: Exception) {
                        Log.e("PrivacyUtils", "百度地图SDK初始化失败: ${e.message}", e)
                        onFailure(e.message ?: "未知错误")
                    }
                }, 500) // 延迟500ms确保隐私政策设置生效
            } catch (e: Exception) {
                Log.e("PrivacyUtils", "设置百度地图隐私政策失败: ${e.message}", e)
                onFailure(e.message ?: "未知错误")
            }
        }
    }
}
