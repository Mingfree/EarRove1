package com.example.earrove.utils

import com.example.earrove.BuildConfig

/**
 * 应用配置类，集中管理配置参数。
 *
 * 敏感配置（API Key 等）通过 BuildConfig 注入，避免硬编码在仓库中。
 */
object AppConfig {

    // ==================== 阿里云百炼 DashScope 配置 ====================

    /**
     * DashScope API Key（语音识别 Paraformer + 通义千问大模型 共用）
     * 获取地址：https://bailian.console.aliyun.com/ → API Key 管理
     * Paraformer 实时语音识别有免费额度
     */
    val DASHSCOPE_API_KEY: String = BuildConfig.DASHSCOPE_API_KEY

    fun isDashScopeConfigured(): Boolean = !DASHSCOPE_API_KEY.startsWith("sk-YOUR")

    // ==================== 百度 TTS 鉴权配置（AIPE SDK，只需 API_KEY + SECRET_KEY） ====================

    val BAIDU_TTS_API_KEY: String = BuildConfig.BAIDU_TTS_API_KEY
    val BAIDU_TTS_SECRET_KEY: String = BuildConfig.BAIDU_TTS_SECRET_KEY

    // ==================== 语音配置 ====================

    /** TTS 语速（0-9） */
    const val TTS_SPEED = "5"
    /** TTS 音量（0-15） */
    const val TTS_VOLUME = "9"
    /** TTS 音调（0-9） */
    const val TTS_PITCH = "5"
    /** 发音人（0=度小美 女声） */
    const val TTS_SPEAKER = "0"

    // ==================== 导航提示语 ====================

    // 导航 TTS 文案历史上曾由常量承载；目前统一由页面/仲裁器通过 strings.xml 提供。

    // ==================== DestinationExtractor 提示词 ====================

    /**
     * 系统提示词：让大模型只返回地名
     */
    const val DESTINATION_EXTRACT_SYSTEM_PROMPT = """你是一个导航助手的目的地提取模块。
用户会用自然语言告诉你他想去哪里，你需要：
1. 从用户的话中提取出目的地的完整地址或地名（可以是地点、建筑、地标、街道门牌号、小区楼栋等）。
2. 尽量保留用户提到的所有地址细节，包括省、市、区、街道、门牌号、楼栋号等，越精确越好。
3. 只返回地址本身，不要加任何解释、标点或多余文字。
4. 如果用户说了多个地方，只取最终目的地。
5. 如果无法识别目的地，返回原文。

示例：
用户：我想去北京天安门广场 → 北京天安门广场
用户：帮我导航到上海虹桥火车站 → 上海虹桥火车站
用户：去北京市海淀区中关村大街27号 → 北京市海淀区中关村大街27号
用户：带我到朝阳区建国路88号SOHO现代城A座 → 朝阳区建国路88号SOHO现代城A座
用户：我要去南京路步行街100号 → 南京路步行街100号
用户：从这里出发去杭州西湖 → 杭州西湖
用户：带我去最近的加油站 → 最近的加油站
用户：天安门 → 天安门"""

    /** 大模型模型名 */
    const val LLM_MODEL = "qwen-flash"
    /** 大模型最大 token */
    const val LLM_MAX_TOKENS = 100
    /** 大模型温度（低温度 = 更确定的结果） */
    const val LLM_TEMPERATURE = 0.1

    // ==================== 火山引擎 Ark API 配置 (Doubao Mini 极速模式) ====================

    /** 火山引擎 Ark API Key（通过 BuildConfig 注入） */
    val ARK_API_KEY: String = BuildConfig.ARK_API_KEY
    /** Ark API 基础地址 */
    const val ARK_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
    /** Doubao Mini 极速模型 */
    const val ARK_MODEL = "doubao-seed-2-0-mini-260215"
    /** OCR 固定提示语 */
    const val ARK_OCR_PROMPT = "识别图中文字内容，包括指示牌、路标、说明书、书本、标签、菜单等，简洁概括，回复不超过100字"

    // ==================== 地理编码配置 ====================

    const val DEFAULT_CITY = "全国"

    // ==================== 调试配置 ====================

    const val DEBUG = true
    const val LOG_TAG_PREFIX = "EarRove_"

    fun getLogTag(className: String): String = "$LOG_TAG_PREFIX$className"
}
