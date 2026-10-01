package com.example.earrove.utils

import android.content.Context
import com.example.earrove.R
import com.example.earrove.domain.arbitration.ArbitrationTextProvider
import com.example.earrove.domain.arbitration.ObstacleType
import com.example.earrove.domain.arbitration.TrafficLightStatus
import com.example.earrove.domain.arbitration.TurnDirection

/**
 * 仲裁层的本地化边界：枚举在此映射为文案资源，向上只暴露已渲染的字符串。
 */
class AndroidArbitrationTextProvider(
    private val context: Context
) : ArbitrationTextProvider {

    override fun obstacleText(obstacleType: ObstacleType): String =
        context.getString(R.string.arb_obstacle_template, context.getString(obstacleType.labelRes()))

    override fun turnText(distanceMeters: Int, turnType: TurnDirection): String =
        context.getString(
            R.string.arb_turn_template,
            NavigationTextFormat.formatDistance(distanceMeters),
            context.getString(turnType.labelRes())
        )

    override fun trafficLightText(status: TrafficLightStatus, countdown: Int): String =
        context.getString(
            R.string.arb_traffic_light_template,
            context.getString(status.labelRes()),
            countdown
        )

    override fun destinationText(name: String): String =
        context.getString(R.string.arb_destination_template, name)

    override fun routeStartText(destination: String, distance: Int, durationMinutes: Int): String =
        context.getString(
            R.string.arb_route_start_template,
            destination,
            NavigationTextFormat.formatDistance(distance),
            NavigationTextFormat.formatDurationMinutes(durationMinutes)
        )
}
