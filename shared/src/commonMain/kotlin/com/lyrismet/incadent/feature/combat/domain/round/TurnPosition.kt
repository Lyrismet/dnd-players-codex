package com.lyrismet.incadent.feature.combat.domain.round

/** where the fight stands: the round and the position of the one who moves */
data class TurnPosition(
    val round: Int,
    val turn: Int,
)
