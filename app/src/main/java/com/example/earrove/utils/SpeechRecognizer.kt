package com.example.earrove.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 语音识别管理器（阿里百炼 DashScope Paraformer 实时 ASR）
 * 通过 OkHttp WebSocket 直连 DashScope，使用 AudioRecord 采集 PCM 音频。
 * 无需额外 SDK。
 */
class SpeechRecognizerManager(context: Context) {

    companion object {
        private val TAG = AppConfig.getLogTag("SpeechRecognizer")

        /** DashScope 实时语音识别 WebSocket 地址 */
        private const val WS_URL =
            "wss://dashscope.aliyuncs.com/api-ws/v1/inference/"

        // 音频参数：16kHz 16bit 单声道 PCM
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        // 每帧 100ms，16kHz × 2字节 × 0.1s = 3200 字节
        private const val FRAME_MS = 100
        private const val FRAME_BYTES = SAMPLE_RATE * 2 * FRAME_MS / 1000

        // 静音检测阈值（RMS）
        private const val SILENCE_RMS_THRESHOLD = 300.0
        // 检测到语音后，连续静音多少帧自动结束（20 × 100ms = 2s）
        private const val SILENCE_FRAMES_AFTER_VOICE = 20
        // 一直没有语音，多少帧后超时（80 × 100ms = 8s）
        private const val NO_VOICE_TIMEOUT_FRAMES = 80
    }

    private val appContext: Context = context.applicationContext
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket 不超时
        .build()

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private var taskId: String? = null

    private val isRunning = AtomicBoolean(false)
    private val taskStarted = AtomicBoolean(false)

    /** 累积最终识别文本 */
    private val finalText = StringBuilder()

    @Volatile
    var isListening = false
        private set

    // ------------------------------------------------------------------ Flow API

    fun startListening(): Flow<String> = callbackFlow {
        if (!AppConfig.isDashScopeConfigured()) {
            Log.w(TAG, "DashScope API Key 未配置")
            trySend("")
            close()
            return@callbackFlow
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "缺少录音权限")
            trySend("")
            close()
            return@callbackFlow
        }

        if (isRunning.get()) {
            Log.w(TAG, "Already running, ignoring duplicate call")
            trySend("")
            close()
            return@callbackFlow
        }

        isRunning.set(true)
        taskStarted.set(false)
        isListening = true
        finalText.setLength(0)
        taskId = UUID.randomUUID().toString()

        val request = Request.Builder()
            .url(WS_URL)
            .header("Authorization", "bearer ${AppConfig.DASHSCOPE_API_KEY}")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                sendRunTask(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text) { result ->
                    trySend(result)
                    cleanup()
                    close()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                cleanup()
                trySend("")
                close()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                cleanup()
            }
        })

        awaitClose {
            stopListeningInternal()
        }
    }

    // ------------------------------------------------------------------ WebSocket 消息

    private fun sendRunTask(ws: WebSocket) {
        try {
            val header = JSONObject().apply {
                put("action", "run-task")
                put("task_id", taskId)
                put("streaming", "duplex")
            }

            val parameters = JSONObject().apply {
                put("format", "pcm")
                put("sample_rate", SAMPLE_RATE)
            }

            val payload = JSONObject().apply {
                put("task_group", "audio")
                put("task", "asr")
                put("function", "recognition")
                put("model", "paraformer-realtime-v2")
                put("parameters", parameters)
                put("input", JSONObject())
            }

            val msg = JSONObject().apply {
                put("header", header)
                put("payload", payload)
            }

            Log.d(TAG, "Sending run-task")
            ws.send(msg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build run-task JSON", e)
        }
    }

    private fun sendFinishTask() {
        val ws = webSocket ?: return
        try {
            val header = JSONObject().apply {
                put("action", "finish-task")
                put("task_id", taskId)
            }

            val payload = JSONObject().apply {
                put("input", JSONObject())
            }

            val msg = JSONObject().apply {
                put("header", header)
                put("payload", payload)
            }

            Log.d(TAG, "Sending finish-task")
            ws.send(msg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build finish-task JSON", e)
        }
    }

    // ------------------------------------------------------------------ 服务端消息处理

    private fun handleServerMessage(text: String, onComplete: (String) -> Unit) {
        try {
            val msg = JSONObject(text)
            val header = msg.getJSONObject("header")
            val event = header.optString("event", "")

            Log.d(TAG, "Event: $event")

            when (event) {
                "task-started" -> {
                    taskStarted.set(true)
                    startAudioCapture()
                }

                "result-generated" -> {
                    handleResult(msg)
                }

                "task-finished" -> {
                    Log.i(TAG, "Task finished")
                    val result = finalText.toString().trim()
                    onComplete(result)
                }

                "task-failed" -> {
                    val errMsg = header.optString("error_message", "未知错误")
                    Log.e(TAG, "Task failed: $errMsg")
                    onComplete("")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse server message: $text", e)
        }
    }

    private fun handleResult(msg: JSONObject) {
        val payload = msg.optJSONObject("payload") ?: return
        val output = payload.optJSONObject("output") ?: return

        // "sentence" 是已确认的完整句子结果
        val sentence = output.optJSONObject("sentence")
        if (sentence != null) {
            val sentenceText = sentence.optString("text", "")
            if (sentenceText.isNotEmpty()) {
                Log.i(TAG, "Sentence: $sentenceText")
                finalText.append(sentenceText)
            }
        }
    }

    // ------------------------------------------------------------------ 音频采集

    private fun startAudioCapture() {
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING),
            FRAME_BYTES * 4
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL, ENCODING, bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed")
            sendFinishTask()
            return
        }

        audioRecord?.startRecording()

        recordThread = Thread({
            val buffer = ByteArray(FRAME_BYTES)
            var silenceCount = 0
            var hasVoice = false

            while (isRunning.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read <= 0) continue
                val ws = webSocket ?: continue

                // 发送 PCM 数据
                ws.send(buffer.toByteString(0, read))

                // 简易能量静音检测
                var energy = 0.0
                var i = 0
                while (i < read - 1) {
                    val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
                    energy += sample.toDouble() * sample.toDouble()
                    i += 2
                }
                energy /= (read / 2)
                val rms = Math.sqrt(energy)

                if (rms > SILENCE_RMS_THRESHOLD) {
                    hasVoice = true
                    silenceCount = 0
                } else {
                    silenceCount++
                }

                // 语音后 2s 静音 → 自动结束
                if (hasVoice && silenceCount >= SILENCE_FRAMES_AFTER_VOICE) {
                    Log.i(TAG, "Silence after voice, auto finishing")
                    sendFinishTask()
                    break
                }

                // 始终无语音 8s → 超时
                if (!hasVoice && silenceCount >= NO_VOICE_TIMEOUT_FRAMES) {
                    Log.i(TAG, "No voice timeout, finishing")
                    sendFinishTask()
                    break
                }
            }

            stopAudioRecord()
        }, "DashScope-AudioCapture")

        recordThread?.start()
    }

    private fun stopAudioRecord() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
    }

    // ------------------------------------------------------------------ 停止 / 释放

    fun stopListening() {
        stopListeningInternal()
    }

    private fun stopListeningInternal() {
        if (isRunning.get()) {
            isRunning.set(false)
            sendFinishTask()
        }
        isListening = false
    }

    fun cancel() {
        isRunning.set(false)
        isListening = false
        webSocket?.cancel()
        webSocket = null
        stopAudioRecord()
    }

    fun destroy() {
        cancel()
        Log.i(TAG, "SpeechRecognizerManager released")
    }

    // ------------------------------------------------------------------ 内部

    private fun cleanup() {
        isRunning.set(false)
        taskStarted.set(false)
        isListening = false
        stopAudioRecord()
    }
}

@Composable
fun rememberSpeechRecognizer(): SpeechRecognizerManager {
    val context = LocalContext.current
    return remember {
        SpeechRecognizerManager(context)
    }
}

@Composable
fun SpeechRecognizerEffect(speechRecognizer: SpeechRecognizerManager) {
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }
}
