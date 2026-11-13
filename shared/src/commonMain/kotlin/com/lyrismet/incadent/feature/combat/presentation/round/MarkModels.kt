package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatMark
import com.lyrismet.incadent.feature.combat.domain.model.CustomCondition
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import com.lyrismet.incadent.feature.combat.domain.round.isExpired

/** what the sheet of a new mark has typed for one participant */
internal data class MarkDraft(
    val memberId: Long,
    // the sheet makes a state of the campaign instead of a mark
    val custom: Boolean = false,
    val text: String = "",
    val concentration: Boolean = false,
    val roundsText: String = "",
)

enum class MarkTone { NORMAL, LAST, EXPIRED }

data class PanelMark(
    val id: Long,
    val label: String,
    val tone: MarkTone,
    val concentration: Boolean,
    // set for a condition that is on, the cross takes it off the same way a tap on its chip does
    val condition: CombatCondition? = null,
)

data class CustomItem(
    val name: String,
    val on: Boolean,
)

data class MarkSheet(
    val name: String,
    val title: String,
    val fieldLabel: String,
    val text: String,
    val custom: Boolean,
    val concentration: Boolean,
    val roundsText: String,
    val canAdd: Boolean,
    val showPresets: Boolean,
)

internal const val MARK_MINUTE_ROUNDS = 10
internal const val MARK_TEN_MINUTES_ROUNDS = 100
internal const val MARK_ROUNDS_DIGITS = 3

/** the marks of a participant in the order the row tells them: ran out, concentration, states, facts */
internal fun List<CombatMark>.inRowOrder(round: Int): List<CombatMark> =
    sortedBy { mark ->
        when {
            mark.isExpired(round) -> RowOrder.EXPIRED
            mark.kind == MarkKind.CONCENTRATION -> RowOrder.CONCENTRATION
            mark.kind == MarkKind.CUSTOM -> RowOrder.CUSTOM
            else -> RowOrder.FACT
        }
    }

private enum class RowOrder { EXPIRED, CONCENTRATION, CUSTOM, FACT }

internal fun CustomCondition.isOn(marks: List<CombatMark>): Boolean =
    marks.any { it.kind == MarkKind.CUSTOM && it.text == name }
