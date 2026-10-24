package com.lyrismet.incadent.feature.codex.presentation.style

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.statusColor
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus

private val DeadLifeForeground = Color(0xFFC9CDD4)
private val DeadLifeBorder = Color(0xFF4A4D5C)

private val Positive =
    statusColor(
        AppPalette.EmeraldBright,
        AppPalette.Emerald.copy(alpha = 0.14f),
        AppPalette.Emerald.copy(alpha = 0.42f),
    )
private val Negative =
    statusColor(AppPalette.MaroonBright, AppPalette.Maroon.copy(alpha = 0.24f), AppPalette.Maroon.copy(alpha = 0.65f))
private val Neutral =
    statusColor(AppPalette.TextMuted, AppPalette.Slate.copy(alpha = 0.12f), AppPalette.Slate.copy(alpha = 0.34f))

fun NpcStatus.toStatusColor(): StatusColor =
    when (this) {
        NpcStatus.FRIEND -> Positive
        NpcStatus.ENEMY -> Negative
        NpcStatus.NEUTRAL -> Neutral
    }

fun NpcLifeState.toStatusColor(): StatusColor =
    when (this) {
        NpcLifeState.ALIVE -> Positive
        NpcLifeState.DEAD -> statusColor(DeadLifeForeground, AppPalette.DeadBackground, DeadLifeBorder)
    }

fun PartyPresence.toStatusColor(): StatusColor =
    when (this) {
        PartyPresence.IN -> PartyMemberColor
        PartyPresence.AWAY -> Neutral
    }

fun QuestStatus.toStatusColor(): StatusColor =
    when (this) {
        QuestStatus.ACTIVE ->
            statusColor(AppPalette.GoldBright, AppPalette.Gold.copy(alpha = 0.13f), AppPalette.Gold.copy(alpha = 0.42f))

        QuestStatus.COMPLETED -> Positive
        QuestStatus.FAILED -> Negative
    }

/** a species chip has no status, it stays the neutral grey */
val SpeciesMentionColor = Neutral

/** location mention chips always use the parchment accent, regardless of any per-entity state */
val LocationMentionColor =
    statusColor(
        foreground = AppPalette.Parchment,
        background = AppPalette.Parchment.copy(alpha = 0.10f),
        border = AppPalette.Parchment.copy(alpha = 0.3f),
    )

/** party members always use the azure accent - the party is the one group of codex entries with its own colour */
val PartyMemberColor =
    statusColor(
        foreground = AppPalette.Azure,
        background = AppPalette.Party.copy(alpha = 0.14f),
        border = AppPalette.Party.copy(alpha = 0.45f),
    )
