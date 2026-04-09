package com.example.earrove.domain.usecase.settings

import com.example.earrove.data.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsInteractorTest {

    private class FakeSettingsRepository : SettingsRepository {
        var rate = 1.0f
        var haptic = true
        var visual = false
        var home: String? = null

        override fun getTtsSpeechRate(): Float = rate
        override fun setTtsSpeechRate(value: Float) {
            rate = value
        }

        override fun isHapticEnabled(): Boolean = haptic
        override fun setHapticEnabled(value: Boolean) {
            haptic = value
        }

        override fun isVisualAssistEnabled(): Boolean = visual
        override fun setVisualAssistEnabled(value: Boolean) {
            visual = value
        }

        override fun getHomeAddress(): String? = home
        override fun setHomeAddress(value: String) {
            home = value
        }

        override fun clearHomeAddress() {
            home = null
        }
    }

    @Test
    fun loadSnapshot_reflectsRepository() {
        val repo = FakeSettingsRepository().apply {
            rate = 1.5f
            haptic = false
            visual = true
            home = "测试地址"
        }
        val sut = SettingsInteractor(repo)
        val snap = sut.loadSnapshot()
        assertEquals(1.5f, snap.ttsSpeechRate, 0.001f)
        assertEquals(false, snap.hapticEnabled)
        assertEquals(true, snap.visualAssistEnabled)
        assertEquals("测试地址", snap.homeAddress)
    }

    @Test
    fun updateTtsSpeechRate_delegatesToRepository() {
        val repo = FakeSettingsRepository()
        val sut = SettingsInteractor(repo)
        sut.updateTtsSpeechRate(1.25f)
        assertEquals(1.25f, repo.rate, 0.001f)
    }
}
