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
    borderAlpha: Float = 0.4f,
): StatusColor =
    StatusColor(foreground = foreground, background = background, border = foreground.copy(alpha = borderAlpha))

fun NpcStatus.toStatusColor(): StatusColor =
    when (this) {
        NpcStatus.FRIEND ->
            statusColor(foreground = AppPalette.EmeraldBright, background = AppPalette.Emerald.copy(alpha = 0.14f))
        NpcStatus.ENEMY ->
            statusColor(foreground = AppPalette.MaroonBright, background = AppPalette.Maroon.copy(alpha = 0.24f))
        NpcStatus.NEUTRAL ->
            statusColor(foreground = AppPalette.TextMuted, background = AppPalette.TextMuted.copy(alpha = 0.12f))
    }

fun NpcLifeState.toStatusColor(): StatusColor =
    when (this) {
        NpcLifeState.ALIVE ->
            statusColor(foreground = AppPalette.EmeraldBright, background = AppPalette.Emerald.copy(alpha = 0.14f))
        NpcLifeState.DEAD ->
            statusColor(foreground = AppPalette.Dead, background = AppPalette.DeadBackground)
    }

fun PartyPresence.toStatusColor(): StatusColor =
    when (this) {
        PartyPresence.IN -> PartyMemberColor
        PartyPresence.AWAY ->
            statusColor(foreground = AppPalette.TextMuted, background = AppPalette.TextMuted.copy(alpha = 0.12f))
    }

fun QuestStatus.toStatusColor(): StatusColor =
    when (this) {
        QuestStatus.ACTIVE ->
            statusColor(foreground = AppPalette.GoldBright, background = AppPalette.Gold.copy(alpha = 0.13f))
        QuestStatus.COMPLETED ->
            statusColor(foreground = AppPalette.EmeraldBright, background = AppPalette.Emerald.copy(alpha = 0.14f))
        QuestStatus.FAILED ->
            statusColor(foreground = AppPalette.MaroonBright, background = AppPalette.Maroon.copy(alpha = 0.24f))
    }

/** location mention chips always use the parchment accent, regardless of any per-entity state */
val LocationMentionColor =
    statusColor(
        foreground = AppPalette.Parchment,
        background = AppPalette.Parchment.copy(alpha = 0.10f),
        borderAlpha = 0.3f,
    )

/** party members always use the azure accent - the party is the one group of codex entries with its own colour */
val PartyMemberColor =
    statusColor(
        foreground = AppPalette.Azure,
        background = AppPalette.Azure.copy(alpha = 0.14f),
        borderAlpha = 0.45f,
    )
