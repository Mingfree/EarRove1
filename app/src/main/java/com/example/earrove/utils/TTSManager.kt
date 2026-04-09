package com.example.earrove.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.MyApplication
import com.example.earrove.domain.arbitration.ArbitrationSpeech

class TTSManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) : ArbitrationSpeech {
    constructor(context: Context) : this(context, SettingsRepositoryImpl(context))

    private val appContext: Context = context.applicationContext

    companion object {
        private val TAG = AppConfig.getLogTag("TTSManager")
    }

    // ============ 百度 TTS (主引擎) ============
    private var baiduSynthesizer: com.baidu.aipe.tts.AipeSpeechSynthesizer? = null
    private var isBaiduInitialized = false

    // ============ Android 系统 TTS (备用引擎) ============
    private var systemTts: TextToSpeech? = null
    private var isSystemTtsReady = false

    private val handler = Handler(Looper.getMainLooper())
    private val uiScope = CoroutineScope(Dispatchers.Main)

    /** 当前是否正在播放TTS */
    val isSpeaking = AtomicBoolean(false)

    /** 当前待播放文本 */
    @Volatile
    private var pendingText = ""

    /** 播放完成回调 */
    @Volatile
    private var onFinishCallback: (() -> Unit)? = null

    /** 是否已初始化（任一引擎可用即为 true） */
    val isInitialized: Boolean
        get() = isBaiduInitialized || isSystemTtsReady

    private var utteranceId = 0

    init {
        // 1. 尝试初始化百度 TTS
        initBaiduTts(context)

        // 2. 同时初始化系统 TTS 作为备用
        initSystemTts(context)
    }

    private fun initBaiduTts(context: Context) {
        try {
            baiduSynthesizer = com.baidu.aipe.tts.AipeSpeechSynthesizer(context.applicationContext)

            baiduSynthesizer?.setSpeechSynthesizerListener { response ->
                when (response.synthesizeType) {
                    com.baidu.tts.client.SynthesizerResponse.SynthesizeType.PLAY_START -> {
                        Log.d(TAG, "百度 TTS play start")
                        isSpeaking.set(true)
                    }
                    com.baidu.tts.client.SynthesizerResponse.SynthesizeType.PLAY_FINISH -> {
                        Log.d(TAG, "百度 TTS play finish")
                        isSpeaking.set(false)
                        handler.post {
                            onFinishCallback?.invoke()
                            onFinishCallback = null
                        }
                    }
                    com.baidu.tts.client.SynthesizerResponse.SynthesizeType.SYNTHESIZE_ERROR -> {
                        val err = response.synthesizerError
                        Log.e(TAG, "百度 TTS error: ${err?.description}")
                        isSpeaking.set(false)
                        handler.post {
                            onFinishCallback?.invoke()
                            onFinishCallback = null
                        }
                    }
                    else -> { /* ignore */ }
                }
            }

            baiduSynthesizer?.setParam(com.baidu.tts.client.SpeechSynthesizer.PARAM_API_KEY, AppConfig.BAIDU_TTS_API_KEY)
            baiduSynthesizer?.setParam(com.baidu.tts.client.SpeechSynthesizer.PARAM_SECRET_KEY, AppConfig.BAIDU_TTS_SECRET_KEY)
            baiduSynthesizer?.setParam(com.baidu.tts.client.SpeechSynthesizer.PARAM_ONLINE_SPEAKER, AppConfig.TTS_SPEAKER)
            baiduSynthesizer?.setParam(com.baidu.tts.client.SpeechSynthesizer.PARAM_ONLINE_TIMEOUT, "3000")

            val err = baiduSynthesizer?.loadOnlineTts()
            if (err != null && err.detailCode == 0) {
                isBaiduInitialized = true
                Log.d(TAG, "百度 TTS 初始化成功")
            } else {
                Log.e(TAG, "百度 TTS 初始化失败: ${err?.detailCode} ${err?.detailMessage}")
                Log.d(TAG, "将使用系统 TTS 作为备用")
            }
        } catch (e: Exception) {
            Log.e(TAG, "百度 TTS 初始化异常: ${e.message}")
        }
    }

    private fun initSystemTts(context: Context) {
        systemTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = systemTts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 尝试简体中文
                    systemTts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                }
                systemTts?.setSpeechRate(settingsRepository.getTtsSpeechRate())
                systemTts?.setPitch(1.0f)

                // 设置播放完成监听
                systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {
                        Log.d(TAG, "系统 TTS 开始播放: $id")
                        isSpeaking.set(true)
                    }

                    override fun onDone(id: String?) {
                        Log.d(TAG, "系统 TTS 播放完成: $id")
                        isSpeaking.set(false)
                        handler.post {
                            onFinishCallback?.invoke()
                            onFinishCallback = null
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        Log.e(TAG, "系统 TTS 播放错误: $id")
                        isSpeaking.set(false)
                        handler.post {
                            onFinishCallback?.invoke()
                            onFinishCallback = null
                        }
                    }
                })

                isSystemTtsReady = true
                Log.d(TAG, "系统 TTS 初始化成功 (备用引擎)")
            } else {
                Log.e(TAG, "系统 TTS 初始化失败: status=$status")
            }
        }
    }

    private fun mapSpeechRateToBaiduSpeedParam(rate: Float): Int {
        val r = rate.coerceIn(0.5f, 2.0f)
        val normalized = (r - 0.5f) / (2.0f - 0.5f) // 0..1
        val raw = kotlin.math.round(normalized * 9f).toInt()
        return raw.coerceIn(0, 9)
    }

    /**
     * 从设置中读取最新语速，并同步到两个引擎（系统 TTS + 百度 TTS）。
     *
     * 说明：百度引擎的参数 key 在 SDK 内部，可能存在也可能不存在；这里用反射尽量兼容，
     * 若 key 不存在则忽略（避免编译/运行崩溃）。
     */
    private fun applySpeechRateFromSettings() {
        val rate = settingsRepository.getTtsSpeechRate()
        if (isSystemTtsReady) {
            systemTts?.setSpeechRate(rate)
        }

        if (isBaiduInitialized) {
            val speed = mapSpeechRateToBaiduSpeedParam(rate)
            runCatching {
                val field = com.baidu.tts.client.SpeechSynthesizer::class.java.getField("PARAM_SPEED")
                val key = field.get(null) as String
                baiduSynthesizer?.setParam(key, speed.toString())
            }
        }
    }

    fun speak(text: String, interrupt: Boolean = true) {
        if (!isInitialized) {
            Log.w(TAG, "TTS 均未初始化，跳过: $text")
            return
        }

        uiScope.launch {
            pendingText = text
            Log.d(TAG, "Speaking: $text")

            applySpeechRateFromSettings()
            if (isBaiduInitialized) {
                // 优先使用百度 TTS
                if (interrupt) {
                    baiduSynthesizer?.stop()
                    isSpeaking.set(false)
                }
                baiduSynthesizer?.speak(com.baidu.tts.client.TtsEntity(text, com.baidu.tts.client.TtsMode.ONLINE))
            } else if (isSystemTtsReady) {
                // 降级到系统 TTS
                if (interrupt) {
                    systemTts?.stop()
                    isSpeaking.set(false)
                }
                val id = "tts_${utteranceId++}"
                systemTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
    }

    fun speakWithoutInterrupt(text: String) {
        if (!isInitialized) return

        uiScope.launch {
            pendingText = text
            applySpeechRateFromSettings()
            if (isBaiduInitialized) {
                baiduSynthesizer?.speak(com.baidu.tts.client.TtsEntity(text, com.baidu.tts.client.TtsMode.ONLINE))
            } else if (isSystemTtsReady) {
                val id = "tts_${utteranceId++}"
                systemTts?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        settingsRepository.setTtsSpeechRate(rate)
        systemTts?.setSpeechRate(rate)
    }

    fun pause() {
        stop()
    }

    fun resume() {
        // 不支持 resume
    }

    override fun stop() {
        baiduSynthesizer?.stop()
        systemTts?.stop()
        isSpeaking.set(false)
        onFinishCallback?.invoke()
        onFinishCallback = null
    }

    /**
     * 播报文字并挂起，直到播放完成或被停止。
     */
    override suspend fun speakAndWait(text: String) {
        if (!isInitialized) return
        suspendCancellableCoroutine { cont ->
            onFinishCallback = {
                if (cont.isActive) cont.resume(Unit)
            }
            cont.invokeOnCancellation {
                stop()
            }
            uiScope.launch {
                pendingText = text
                applySpeechRateFromSettings()
                if (isBaiduInitialized) {
                    baiduSynthesizer?.stop()
                    baiduSynthesizer?.speak(com.baidu.tts.client.TtsEntity(text, com.baidu.tts.client.TtsMode.ONLINE))
                } else if (isSystemTtsReady) {
                    systemTts?.stop()
                    val id = "tts_${utteranceId++}"
                    systemTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                }
            }
        }
    }

    fun release() {
        baiduSynthesizer?.stop()
        baiduSynthesizer?.release()
        baiduSynthesizer = null
        isBaiduInitialized = false

        systemTts?.stop()
        systemTts?.shutdown()
        systemTts = null
        isSystemTtsReady = false
    }
}

@Composable
fun rememberTTSManager(): TTSManager {
    val context = LocalContext.current
    val app = context.applicationContext as? MyApplication
    return remember(context) {
        TTSManager(context, app?.container?.settingsRepository ?: SettingsRepositoryImpl(context))
    }
}

@Composable
fun TTSManagerEffect(ttsManager: TTSManager) {
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.release()
        }
    }
}
