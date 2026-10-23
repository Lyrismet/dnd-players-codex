package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.results.HitRange
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_species_fights_format
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

/** the hit points the fights hint at: "8-12", "≤ 12" when only a fall was seen, "≥ 6" when only a survivor */
fun HitRange.text(): String {
    val low = low
    val high = high
    return when {
        low != null && high != null -> if (low >= high) high.toString() else "$low-$high"
        high != null -> "≤ $high"
        low != null -> "≥ $low"
        else -> ""
    }
}
