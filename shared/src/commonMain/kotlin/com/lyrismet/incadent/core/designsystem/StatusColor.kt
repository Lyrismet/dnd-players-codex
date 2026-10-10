package com.lyrismet.incadent.core.designsystem

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.QuestStatus

/** foreground/background/border triple for a status chip, e.g. the NPC status badge or a session-note mention */
data class StatusColor(
    val foreground: Color,
    val background: Color,
    val border: Color,
)

private fun statusColor(
    foreground: Color,
    background: Color,
    border: Color = foreground.copy(alpha = 0.4f),
): StatusColor = StatusColor(foreground = foreground, background = background, border = border)

// Players Codex v6 `ST` / `LIFE` / `NEU_C` / `PARTY_C`: borders come from the base accent, not the bright foreground
private val Slate = Color(0xFF94A3B8)
private val DeadLifeForeground = Color(0xFFC9CDD4)
private val DeadLifeBorder = Color(0xFF4A4D5C)
private val PartyBase = Color(0xFF6EA0E6)

private val Positive =
    statusColor(
        AppPalette.EmeraldBright,
        AppPalette.Emerald.copy(alpha = 0.14f),
        AppPalette.Emerald.copy(alpha = 0.42f),
    )
private val Negative =
    statusColor(AppPalette.MaroonBright, AppPalette.Maroon.copy(alpha = 0.24f), AppPalette.Maroon.copy(alpha = 0.65f))
private val Neutral =
    statusColor(AppPalette.TextMuted, Slate.copy(alpha = 0.12f), Slate.copy(alpha = 0.34f))

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
        background = PartyBase.copy(alpha = 0.14f),
        border = PartyBase.copy(alpha = 0.45f),
    )
