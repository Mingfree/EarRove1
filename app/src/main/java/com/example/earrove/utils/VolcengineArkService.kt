package com.example.earrove.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 火山引擎 Ark API 服务
 * 使用 Doubao-seed-2-0-mini 极速模型进行图像识别
 * 通过 Chat Completions API 调用
 */
class VolcengineArkService {

    companion object {
        private val TAG = AppConfig.getLogTag("VolcengineArk")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 使用 Chat Completions API 识别图像
     * @param base64Image Base64 编码的图像数据 (不含 data:image 前缀)
     * @return 识别结果文本
     */
    suspend fun recognizeImage(base64Image: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始调用火山引擎 Ark API 识别图像...")

            // 构造图片数据 URL
            val imageDataUrl = "data:image/jpeg;base64,$base64Image"

            // 构造 Chat Completions 请求体
            val requestBody = buildChatCompletionsBody(imageDataUrl)

            Log.d(TAG, "请求体构造完成，图片数据长度: ${base64Image.length}")

            val request = Request.Builder()
                .url("${AppConfig.ARK_BASE_URL}/chat/completions")
                .addHeader("Authorization", "Bearer ${AppConfig.ARK_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "API 请求失败: ${response.code} - $responseBody")
                return@withContext Result.failure(
                    Exception("API 请求失败 (${response.code})")
                )
            }

            // 解析响应
            val resultText = parseChatCompletionsResult(responseBody)
            Log.d(TAG, "识别结果: $resultText")
            Result.success(resultText)

        } catch (e: Exception) {
            Log.e(TAG, "图像识别异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 构造 Chat Completions API 请求体
     */
    private fun buildChatCompletionsBody(imageDataUrl: String): String {
        val imageContent = JSONObject().apply {
            put("type", "image_url")
            put("image_url", JSONObject().apply {
                put("url", imageDataUrl)
            })
        }

        val textContent = JSONObject().apply {
            put("type", "text")
            put("text", AppConfig.ARK_OCR_PROMPT)
        }

        val contentArray = JSONArray().apply {
            put(imageContent)
            put(textContent)
        }

        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", contentArray)
        }

        val messagesArray = JSONArray().apply {
            put(userMessage)
        }

        val body = JSONObject().apply {
            put("model", AppConfig.ARK_MODEL)
            put("messages", messagesArray)
        }

        return body.toString()
    }

    /**
     * 解析 Chat Completions API 响应
     */
    private fun parseChatCompletionsResult(responseBody: String): String {
        try {
            val json = JSONObject(responseBody)

            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                if (message != null) {
                    val content = message.optString("content", "")
                    if (content.isNotEmpty()) {
                        return content
                    }
                }
            }

            Log.w(TAG, "未能从响应中提取结果: $responseBody")
            return "无法解析识别结果"

        } catch (e: Exception) {
            Log.e(TAG, "解析响应异常: ${e.message}", e)
            return "解析识别结果失败"
        }
    }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
    }
}
