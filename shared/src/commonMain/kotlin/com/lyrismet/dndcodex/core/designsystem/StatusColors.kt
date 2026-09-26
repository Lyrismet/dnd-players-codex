package com.lyrismet.dndcodex.core.designsystem

import androidx.compose.ui.graphics.Color
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus

/** foreground/background pair for a status chip, e.g. the NPC status badge or a session-note mention */
data class StatusColor(
    val foreground: Color,
    val background: Color,
)

fun NpcStatus.toStatusColor(): StatusColor =
    when (this) {
        NpcStatus.FRIEND -> StatusColor(AppPalette.EmeraldBright, AppPalette.Emerald.copy(alpha = 0.14f))
        NpcStatus.ENEMY -> StatusColor(AppPalette.MaroonBright, AppPalette.Maroon.copy(alpha = 0.24f))
        NpcStatus.NEUTRAL -> StatusColor(AppPalette.TextMuted, AppPalette.TextMuted.copy(alpha = 0.12f))
        NpcStatus.DEAD -> StatusColor(AppPalette.Dead, AppPalette.DeadBackground)
    }

fun QuestStatus.toStatusColor(): StatusColor =
    when (this) {
        QuestStatus.ACTIVE -> StatusColor(AppPalette.GoldBright, AppPalette.Gold.copy(alpha = 0.13f))
        QuestStatus.COMPLETED -> StatusColor(AppPalette.EmeraldBright, AppPalette.Emerald.copy(alpha = 0.14f))
        QuestStatus.FAILED -> StatusColor(AppPalette.MaroonBright, AppPalette.Maroon.copy(alpha = 0.24f))
    }

/** location mention chips always use the parchment accent, regardless of any per-entity state */
val LocationMentionColor = StatusColor(AppPalette.Parchment, AppPalette.Parchment.copy(alpha = 0.10f))
