package com.example.earrove.domain.validation

import java.lang.Character

/**
 * 家地址输入：规范化与离线格式校验（不含地图语义）。
 */
object HomeAddressInputRules {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 200

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
        return true
    }
}
