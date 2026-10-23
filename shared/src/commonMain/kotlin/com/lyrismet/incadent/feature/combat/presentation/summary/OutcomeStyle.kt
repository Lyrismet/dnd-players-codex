package com.lyrismet.incadent.feature.combat.presentation.summary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_outcome_interrupted
import dndplayerscodex.shared.generated.resources.combat_outcome_peace
import dndplayerscodex.shared.generated.resources.combat_outcome_retreat
import dndplayerscodex.shared.generated.resources.combat_outcome_victory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun CombatOutcome.label(): StringResource =
    when (this) {
        CombatOutcome.VICTORY -> Res.string.combat_outcome_victory
        CombatOutcome.RETREAT -> Res.string.combat_outcome_retreat
        CombatOutcome.PEACE -> Res.string.combat_outcome_peace
        CombatOutcome.INTERRUPTED -> Res.string.combat_outcome_interrupted
    }

fun CombatOutcome.toStatusColor(): StatusColor =
    when (this) {
        CombatOutcome.VICTORY -> QuestStatus.COMPLETED.toStatusColor()
        CombatOutcome.RETREAT -> QuestStatus.ACTIVE.toStatusColor()
        CombatOutcome.PEACE -> PartyMemberColor
        CombatOutcome.INTERRUPTED -> NpcStatus.NEUTRAL.toStatusColor()
    }

/** the outcome as the small coloured badge of the journal row and the session card */
@Composable
fun OutcomeBadge(
    outcome: CombatOutcome,
    modifier: Modifier = Modifier,
) {
    val color = outcome.toStatusColor()
    TagChip(
        text = stringResource(outcome.label()),
        foreground = color.foreground,
        background = color.background,
        border = color.border,
        modifier = modifier,
    )
}
