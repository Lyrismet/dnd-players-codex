package com.lyrismet.incadent.feature.combat.domain.round

/** what keeps the fight from starting, or how many take part in it */
sealed interface StartReadiness {
    data object NeedEnemy : StartReadiness

    data class NeedOrder(
        val left: Int,
    ) : StartReadiness

    data class Ready(
        val count: Int,
    ) : StartReadiness
}
