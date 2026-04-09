package com.example.earrove.utils

import android.content.Context
import com.example.earrove.R
import com.example.earrove.domain.arbitration.ArbitrationTextProvider

class AndroidArbitrationTextProvider(
    private val context: Context
) : ArbitrationTextProvider {
    override fun obstacleText(obstacleType: String): String =
        context.getString(R.string.arb_obstacle_template, obstacleType)

    override fun turnText(distanceMeters: Int, direction: String): String =
        context.getString(R.string.arb_turn_template, distanceMeters, direction)

    override fun trafficLightText(status: String, countdown: Int): String =
        context.getString(R.string.arb_traffic_light_template, status, countdown)

    override fun destinationText(name: String): String =
        context.getString(R.string.arb_destination_template, name)

    override fun routeStartText(destination: String, distance: Int, durationMinutes: Int): String =
        context.getString(
            R.string.arb_route_start_template,
            destination,
            distance,
            durationMinutes
        )
}

