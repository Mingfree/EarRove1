package com.example.earrove.domain.arbitration

/**
 * 障碍物类型。仲裁层据此选择播报文案，避免以中文字符串在层间传递。
 *
 * 注意：本层不持有 `R.string` 引用；枚举到文案的映射由 UI/utils 层的
 * 本地化边界（如 `AndroidArbitrationTextProvider`）负责。
 */
enum class ObstacleType {
    VEHICLE,
    PEDESTRIAN,
    POLE,
    WALL,
    STAIRS,
    NONE
}

/**
 * 红绿灯状态。仲裁层据此选择播报文案。
 */
enum class TrafficLightStatus {
    RED,
    GREEN,
    YELLOW,
    NONE
}

/**
 * 转向类型。由导航指令解析得到，供仲裁层选择播报文案。
 */
enum class TurnDirection {
    LEFT,
    RIGHT,
    STRAIGHT,
    ARRIVE,
    CONTINUE
}
