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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
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

    /** 播放完成回调；按发起顺序登记，完成/出错/停止时只唤醒对应条目 */
    private val pendingWaits = ConcurrentLinkedQueue<CompletableDeferred<Unit>>()

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

    /**
     * 唤醒最早一个等待中的 [speakAndWait] 调用方。
     *
     * 之所以只唤醒一个：TTS 自身按提交顺序播放，一次「播放完成」只对应最早那次请求。
     * 若唤醒全部，后发起的调用会提前返回，重演原单槽回调互相抢用的缺陷。
     */
    private fun signalNextWaiter() {
        pendingWaits.poll()?.complete(Unit)
    }

    /** 唤醒并清空所有等待者（用于 stop：当前播报被打断，无人应继续等待） */
    private fun signalAllWaiters() {
        while (true) {
            val waiter = pendingWaits.poll() ?: break
            waiter.complete(Unit)
        }
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
                        handler.post { signalNextWaiter() }
                    }
                    com.baidu.tts.client.SynthesizerResponse.SynthesizeType.SYNTHESIZE_ERROR -> {
                        val err = response.synthesizerError
                        Log.e(TAG, "百度 TTS error: ${err?.description}")
                        isSpeaking.set(false)
                        handler.post { signalNextWaiter() }
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
                        handler.post { signalNextWaiter() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        Log.e(TAG, "系统 TTS 播放错误: $id")
                        isSpeaking.set(false)
                        handler.post { signalNextWaiter() }
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
     * 若 key 不存在则忽略，避免编译/运行崩溃。
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
                // 优先使用百度 TTS，统一语音风格
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
        // 当前不支持断点续播，后续若有需求再引入队列状态
    }

    override fun stop() {
        baiduSynthesizer?.stop()
        systemTts?.stop()
        isSpeaking.set(false)
        // 当前播报已被打断，所有等待者都不应继续挂起
        signalAllWaiters()
    }

    /**
     * 播报文字并挂起，直到播放完成或被停止。
     *
     * 未初始化时直接返回（不挂起）。等待者登记在队列中，由播放完成回调逐个唤醒；
     * 这样并发调用不会互相抢用回调（原实现为单一回调槽）。
     */
    override suspend fun speakAndWait(text: String) {
        if (!isInitialized) return
        val waiter = CompletableDeferred<Unit>()
        pendingWaits.add(waiter)
        try {
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
            waiter.await()
        } finally {
            pendingWaits.remove(waiter)
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
