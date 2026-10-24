package com.lyrismet.incadent.feature.combat.domain.round

private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60

/** how long the fight has gone: "03:07", and "1:02:09" after the first hour */
fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val minutes = seconds / SECONDS_IN_MINUTE
    val hours = minutes / MINUTES_IN_HOUR
    val ss = (seconds % SECONDS_IN_MINUTE).toString().padStart(2, '0')
    val mm = (minutes % MINUTES_IN_HOUR).toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}
