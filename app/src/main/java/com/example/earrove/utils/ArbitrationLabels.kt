package com.example.earrove.utils

import androidx.annotation.StringRes
import com.example.earrove.R
import com.example.earrove.domain.arbitration.ObstacleType
import com.example.earrove.domain.arbitration.TrafficLightStatus
import com.example.earrove.domain.arbitration.TurnDirection

/**
 * 仲裁层枚举到文案资源的映射，收口在此以避免多处 when 分支漂移。
 * 领域层（[com.example.earrove.domain.arbitration]）刻意不依赖 `R`。
 */
@StringRes
fun ObstacleType.labelRes(): Int = when (this) {
    ObstacleType.VEHICLE -> R.string.obstacle_vehicle
    ObstacleType.PEDESTRIAN -> R.string.obstacle_pedestrian
    ObstacleType.POLE -> R.string.obstacle_pole
    ObstacleType.WALL -> R.string.obstacle_wall
    ObstacleType.STAIRS -> R.string.obstacle_stairs
    ObstacleType.NONE -> R.string.obstacle_unknown
}

@StringRes
fun TrafficLightStatus.labelRes(): Int = when (this) {
    TrafficLightStatus.RED -> R.string.nav_traffic_light_red
    TrafficLightStatus.GREEN -> R.string.nav_traffic_light_green
    TrafficLightStatus.YELLOW -> R.string.nav_traffic_light_yellow
    TrafficLightStatus.NONE -> R.string.nav_traffic_light_unknown
}

@StringRes
fun TurnDirection.labelRes(): Int = when (this) {
    TurnDirection.LEFT -> R.string.nav_turn_left
    TurnDirection.RIGHT -> R.string.nav_turn_right
    TurnDirection.STRAIGHT -> R.string.nav_turn_straight
    TurnDirection.ARRIVE -> R.string.nav_turn_arrive
    TurnDirection.CONTINUE -> R.string.nav_turn_continue_forward
}
