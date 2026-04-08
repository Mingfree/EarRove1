package com.example.earrove.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

/**
 * 目的地提取器
 * 使用 DashScope 通义千问大模型，从用户自然语言中提取目的地名称，
 * 返回适合传给百度地图地理编码的干净地名。
 */
class DestinationExtractor(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private val TAG = AppConfig.getLogTag("DestinationExtractor")

        /** DashScope OpenAI 兼容接口 */
        private const val API_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * 从用户语音识别原文中提取目的地名称
     *
     * @param rawText ASR 识别的原始文本
     * @return 提取后的干净地名，失败时返回原文
     */
    suspend fun extract(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return trimmed

        if (!AppConfig.isDashScopeConfigured()) {
            Log.w(TAG, "DashScope API Key 未配置，直接返回原文")
            return trimmed
        }

        return withContext(Dispatchers.IO) {
            try {
                val result = callApi(trimmed)
                if (result.isNotBlank()) {
                    Log.i(TAG, "Extracted: \"$trimmed\" → \"$result\"")
                    result
                } else {
                    Log.w(TAG, "LLM returned empty, fallback to raw text")
                    trimmed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extract failed: ${e.message}, fallback to raw text")
                trimmed
            }
        }
    }

    private suspend fun callApi(userText: String): String =
        suspendCancellableCoroutine { cont ->
            val body = buildRequestBody(userText)
            val request = Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer ${AppConfig.DASHSCOPE_API_KEY}")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_TYPE))
                .build()

            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume("")
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        if (!it.isSuccessful || it.body == null) return@use ""
                        parseResponse(it.body!!.string())
                    }
                    if (cont.isActive) cont.resume(result)
                }
            })
        }

    private fun buildRequestBody(userText: String): JSONObject {
        val systemMsg = JSONObject().apply {
            put("role", "system")
            put("content", AppConfig.DESTINATION_EXTRACT_SYSTEM_PROMPT)
        }
        val userMsg = JSONObject().apply {
            put("role", "user")
            put("content", userText)
        }
        val messages = JSONArray().apply {
            put(systemMsg)
            put(userMsg)
        }
        return JSONObject().apply {
            put("model", AppConfig.LLM_MODEL)
            put("messages", messages)
            put("max_tokens", AppConfig.LLM_MAX_TOKENS)
            put("temperature", AppConfig.LLM_TEMPERATURE)
        }
    }

    private fun parseResponse(json: String): String {
        return try {
            val resp = JSONObject(json)
            val choices = resp.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
            message.optString("content", "").trim()
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            ""
        }
    }
}
