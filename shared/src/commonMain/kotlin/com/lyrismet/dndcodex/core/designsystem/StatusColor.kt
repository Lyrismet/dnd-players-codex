package com.lyrismet.dndcodex.core.designsystem

import androidx.compose.ui.graphics.Color
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus

/** foreground/background/border triple for a status chip, e.g. the NPC status badge or a session-note mention */
data class StatusColor(
    val foreground: Color,
    val background: Color,
    val border: Color,
)

fun NpcStatus.toStatusColor(): StatusColor =
    when (this) {
        NpcStatus.FRIEND ->
            StatusColor(
                foreground = AppPalette.EmeraldBright,
                background = AppPalette.Emerald.copy(alpha = 0.14f),
                border = AppPalette.EmeraldBright.copy(alpha = 0.4f),
            )
        NpcStatus.ENEMY ->
            StatusColor(
                foreground = AppPalette.MaroonBright,
                background = AppPalette.Maroon.copy(alpha = 0.24f),
                border = AppPalette.MaroonBright.copy(alpha = 0.4f),
            )
        NpcStatus.NEUTRAL ->
            StatusColor(
                foreground = AppPalette.TextMuted,
                background = AppPalette.TextMuted.copy(alpha = 0.12f),
                border = AppPalette.TextMuted.copy(alpha = 0.4f),
            )
        NpcStatus.DEAD ->
            StatusColor(
                foreground = AppPalette.Dead,
                background = AppPalette.DeadBackground,
                border = AppPalette.Dead.copy(alpha = 0.4f),
            )
    }

fun QuestStatus.toStatusColor(): StatusColor =
    when (this) {
        QuestStatus.ACTIVE ->
            StatusColor(
                foreground = AppPalette.GoldBright,
                background = AppPalette.Gold.copy(alpha = 0.13f),
                border = AppPalette.GoldBright.copy(alpha = 0.4f),
            )
        QuestStatus.COMPLETED ->
            StatusColor(
                foreground = AppPalette.EmeraldBright,
                background = AppPalette.Emerald.copy(alpha = 0.14f),
                border = AppPalette.EmeraldBright.copy(alpha = 0.4f),
            )
        QuestStatus.FAILED ->
            StatusColor(
                foreground = AppPalette.MaroonBright,
                background = AppPalette.Maroon.copy(alpha = 0.24f),
                border = AppPalette.MaroonBright.copy(alpha = 0.4f),
            )
    }

/** location mention chips always use the parchment accent, regardless of any per-entity state */
val LocationMentionColor =
    StatusColor(
        foreground = AppPalette.Parchment,
        background = AppPalette.Parchment.copy(alpha = 0.10f),
        border = AppPalette.Parchment.copy(alpha = 0.3f),
    )
