package com.example.earrove.domain.model

/**
 * 设置页加载用快照，便于 ViewModel / 单测断言。
 */
data class SettingsSnapshot(
    val ttsSpeechRate: Float,
    val hapticEnabled: Boolean,
    val visualAssistEnabled: Boolean,
    val homeAddress: String
)
