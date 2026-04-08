package com.example.earrove.utils

import android.content.Context
import com.example.earrove.BuildConfig
import com.example.earrove.R

/**
 * 启动自检：检查关键三方能力所需配置是否仍为占位符。
 *
 * 说明：
 * - 我们在 `app/build.gradle.kts` 中用 `local.properties` 注入 BuildConfig。
 * - 当 local.properties 缺失对应 key 时，会使用默认占位值（例如 `YOUR-*` / `sk-YOUR-*`）。
 */
object ConfigValidator {

    data class CheckResult(
        val missing: List<String>,
        val isOk: Boolean
    )

    private fun isDashScopeMissing(): Boolean {
        val v = BuildConfig.DASHSCOPE_API_KEY
        return v.isBlank() || v.startsWith("sk-YOUR")
    }

    private fun isYourMissing(v: String): Boolean {
        return v.isBlank() || v.startsWith("YOUR-")
    }

    fun checkEssentialConfig(context: Context): CheckResult {
        val missing = mutableListOf<String>()

        if (isDashScopeMissing()) {
            missing += context.getString(R.string.cfg_missing_dashscope)
        }

        val mapKey = BuildConfig.BAIDU_MAP_API_KEY
        if (isYourMissing(mapKey)) {
            missing += context.getString(R.string.cfg_missing_baidu_map)
        }

        val baiduTtsApiKey = BuildConfig.BAIDU_TTS_API_KEY
        val baiduTtsSecretKey = BuildConfig.BAIDU_TTS_SECRET_KEY
        if (isYourMissing(baiduTtsApiKey) || isYourMissing(baiduTtsSecretKey)) {
            missing += context.getString(R.string.cfg_missing_baidu_tts)
        }

        val arkApiKey = BuildConfig.ARK_API_KEY
        if (isYourMissing(arkApiKey)) {
            missing += context.getString(R.string.cfg_missing_ark)
        }

        return CheckResult(
            missing = missing,
            isOk = missing.isEmpty()
        )
    }
}

