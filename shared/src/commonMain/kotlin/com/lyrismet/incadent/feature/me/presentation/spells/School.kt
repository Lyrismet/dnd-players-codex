package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_school_abjuration
import incadent.shared.generated.resources.me_spells_school_conjuration
import incadent.shared.generated.resources.me_spells_school_divination
import incadent.shared.generated.resources.me_spells_school_enchantment
import incadent.shared.generated.resources.me_spells_school_evocation
import incadent.shared.generated.resources.me_spells_school_illusion
import incadent.shared.generated.resources.me_spells_school_necromancy
import incadent.shared.generated.resources.me_spells_school_transmutation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// the eight schools in the order of the sheet, each with its own stroke on the 24 grid and its own colour
internal class School(
    val name: StringResource,
    val path: String,
    val color: Color,
)

internal val Schools =
    listOf(
        School(
            Res.string.me_spells_school_abjuration,
            "M12 3l7 3v5.5c0 4.4-3 8-7 9.5-4-1.5-7-5.1-7-9.5V6z M12 8.5l3 3.5-3 3.5-3-3.5z",
            Color(0xFF8CB8EC),
        ),
        School(
            Res.string.me_spells_school_conjuration,
            "M4 18a8 3 0 1 0 16 0a8 3 0 1 0-16 0 M12 16V4 M8.5 7.5L12 4l3.5 3.5 " +
                "M7.5 12.5l-1-1 M16.5 12.5l1-1",
            Color(0xFFF0C05A),
        ),
        School(
            Res.string.me_spells_school_divination,
            "M12 3a7 7 0 1 0 0 14a7 7 0 1 0 0-14z M7 21h10 M8.5 16.5L7.5 21 M15.5 16.5l1 4.5 " +
                "M9.5 9a2.8 2.8 0 0 1 2.5-2.5",
            Color(0xFF9FD8F0),
        ),
        School(
            Res.string.me_spells_school_enchantment,
            "M13.5 10.5a1 1 0 0 1 1 1a2 2 0 0 1-2 2a3 3 0 0 1-3-3a4 4 0 0 1 4-4a5 5 0 0 1 5 5 " +
                "a6 6 0 0 1-6 6a7 7 0 0 1-7-7",
            Color(0xFFFF8CC6),
        ),
        School(
            Res.string.me_spells_school_evocation,
            "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z M12 2v4 M12 18v4 M2 12h4 M18 12h4 " +
                "M4.9 4.9l2.8 2.8 M16.3 16.3l2.8 2.8 M4.9 19.1l2.8-2.8 M16.3 7.7l2.8-2.8",
            Color(0xFFFF8A3D),
        ),
        School(
            Res.string.me_spells_school_illusion,
            "M4 5c5 2 11 2 16 0v6c0 5-3.5 9-8 9s-8-4-8-9z M8.5 10.5c.8-.6 1.7-.6 2.5 0 M13 10.5c.8-.6 1.7-.6 2.5 0 " +
                "M9.5 15c1.5 1 3.5 1 5 0",
            Color(0xFFB9A2F2),
        ),
        School(
            Res.string.me_spells_school_necromancy,
            "M6 21V10a6 6 0 0 1 12 0v11z M4 21h16 M12 8v7 M9.5 10.5h5",
            Color(0xFF5ED6B8),
        ),
        School(
            Res.string.me_spells_school_transmutation,
            "M4 12a8 8 0 0 1 13.7-5.6 M20 12a8 8 0 0 1-13.7 5.6 M17.7 2.5v4h-4 M6.3 21.5v-4h4 M12 10l2 2-2 2-2-2z",
            Color(0xFFD4E13A),
        ),
    )

/** the look of a school by its name as written in the spell, null for a school of the player's own words */
@Composable
internal fun schoolOf(name: String): School? =
    Schools.firstOrNull { stringResource(it.name).equals(name.trim(), ignoreCase = true) }
