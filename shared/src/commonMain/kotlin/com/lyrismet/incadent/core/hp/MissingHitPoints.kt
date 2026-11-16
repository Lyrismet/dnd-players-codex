package com.lyrismet.incadent.core.hp

/** the hit points a heal to the maximum adds, never below zero */
fun missingHitPoints(
    current: Int,
    max: Int,
): Int = (max - current).coerceAtLeast(0)
