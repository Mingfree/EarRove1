package com.example.earrove.domain.validation

import java.lang.Character

/**
 * 家地址输入：规范化与离线格式校验（不含地图语义）。
 */
object HomeAddressInputRules {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 200

    /** 连续拉丁字母（如 sha、abc），用于识别「sha忒啦」类乱码 */
    private val latinLetterRun = Regex("[a-zA-Z]+")

    fun normalize(raw: String): String {
        return raw
            .replace('\t', ' ')
            .trim()
            .filter { ch -> !ch.isISOControl() }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun isFormatValid(normalized: String): Boolean {
        if (normalized.length < MIN_LENGTH || normalized.length > MAX_LENGTH) return false
        if (!normalized.any { Character.isIdeographic(it.code) }) return false
        if ('\uFFFD' in normalized) return false
        if (looksLikeLatinNoiseWithFewIdeographs(normalized)) return false
        return true
    }

    /**
     * 拉丁字母段过长且表意文字偏少时视为乱码/无效地址（仍会过「含汉字」校验的如「sha忒啦」）。
     */
    private fun looksLikeLatinNoiseWithFewIdeographs(s: String): Boolean {
        val hanCount = s.count { Character.isIdeographic(it.code) }
        val maxLatinRun = latinLetterRun.findAll(s).map { it.value.length }.maxOrNull() ?: 0
        val latinLetterCount = s.count { it in 'a'..'z' || it in 'A'..'Z' }
        if (maxLatinRun >= 3 && hanCount <= 4) return true
        if (latinLetterCount >= 3 && hanCount <= 4 && latinLetterCount > hanCount) return true
        return false
    }
}
