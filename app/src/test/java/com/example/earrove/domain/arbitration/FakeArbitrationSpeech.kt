package com.example.earrove.domain.arbitration

/**
 * 单测用静默语音实现，不触发系统/百度 TTS。
 */
class FakeArbitrationSpeech : ArbitrationSpeech {
    val speakAndWaitTexts = mutableListOf<String>()
    var stopCount = 0

    override suspend fun speakAndWait(text: String) {
        speakAndWaitTexts.add(text)
    }

    override fun stop() {
        stopCount++
    }
}
