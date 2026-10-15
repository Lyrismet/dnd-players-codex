package com.lyrismet.incadent.core.codexgroup

private const val MAX_RECENT_PLACES = 12

/** the places picked last come first, each once, only the latest few are kept for the select sheet's shortcut row */
fun List<Long>.withRecentPlaces(picked: List<Long>): List<Long> = (picked + this).distinct().take(MAX_RECENT_PLACES)
