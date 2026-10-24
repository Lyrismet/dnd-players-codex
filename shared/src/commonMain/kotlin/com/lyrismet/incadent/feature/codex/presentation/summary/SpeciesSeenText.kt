package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.results.HitRange
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_species_fights_format
import dndplayerscodex.shared.generated.resources.codex_species_row_hp_format
import dndplayerscodex.shared.generated.resources.codex_species_row_seen_format
import dndplayerscodex.shared.generated.resources.codex_species_seen_prefix
import dndplayerscodex.shared.generated.resources.mention_count_in_sessions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** "в 3 сессиях · в боях 2", empty when neither a note nor a fight has met the species */
@Composable
fun speciesSeenLine(
    sessions: Int,
    fights: Int,
): String {
    val parts =
        listOfNotNull(
            if (sessions > 0) {
                stringResource(
                    Res.string.codex_species_row_seen_format,
                    pluralStringResource(Res.plurals.mention_count_in_sessions, sessions, sessions),
                )
            } else {
                null
            },
            if (fights > 0) stringResource(Res.string.codex_species_fights_format, fights) else null,
        )
    return parts.joinToString(" · ")
}

/** the line of the card, "Встречено в 3 сессиях · в боях 2" */
@Composable
fun speciesSeenCardLine(
    sessions: Int,
    fights: Int,
): String {
    val line = speciesSeenLine(sessions, fights)
    return if (line.isEmpty()) "" else stringResource(Res.string.codex_species_seen_prefix, line)
}

/** the hit points the fights hint at: "8-12", or the one bound that a lone fall or survivor gave */
fun HitRange.text(): String {
    val low = low
    val high = high
    return when {
        low != null && high != null -> if (low >= high) high.toString() else "$low-$high"
        high != null -> high.toString()
        low != null -> low.toString()
        else -> ""
    }
}

/** the hint as the cards write it, "≈ 8-12" */
fun HitRange.approxText(): String = "≈ ${text()}"

/** "хиты 12" when the player knows them, "хиты ≈ 8-12" when only fights hint, empty when nothing does */
@Composable
fun speciesHitsLine(
    hitPoints: Int?,
    hitRange: HitRange?,
): String {
    val value = hitPoints?.toString() ?: hitRange?.approxText() ?: return ""
    return stringResource(Res.string.codex_species_row_hp_format, value)
}
