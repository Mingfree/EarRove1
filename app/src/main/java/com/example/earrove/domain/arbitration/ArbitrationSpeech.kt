package com.example.earrove.domain.arbitration

/**
 * 仲裁器所需的语音能力抽象，便于单元测试注入静默实现。
 */
interface ArbitrationSpeech {
    suspend fun speakAndWait(text: String)
    fun stop()
}
