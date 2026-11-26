package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges

private val LEVEL_GAIN_RANGE = 0..99

/** what the level up sheet shows after [event], the levels stop at the top level */
internal fun MeOverlay.LevelUp.reduceLevelUp(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.NumberStepped -> copy(gain = gain.stepped(event.delta, LEVEL_GAIN_RANGE))
        is OverlayEvent.LevelsStepped ->
            copy(levels = levels.stepped(event.delta, 1..maxOf(1, PartyStatRanges.level.last - from)))
        is OverlayEvent.ClassLevelsStepped -> {
            val others = added.filterIndexed { i, _ -> i != event.index }.sum()
            val room = (PartyStatRanges.level.last - from - others).coerceAtLeast(0)
            val next = added.toMutableList().also { it[event.index] = it[event.index].stepped(event.delta, 0..room) }
            copy(added = next, levels = next.sum())
        }
        else -> this
    }
