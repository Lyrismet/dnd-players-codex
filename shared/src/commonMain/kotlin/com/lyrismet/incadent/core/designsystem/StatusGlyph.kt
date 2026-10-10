package com.lyrismet.incadent.core.designsystem

import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus

/** the glyph in a status seal's diamond (Players Codex v6 `GL`), a plain diamond when the status has none */
const val DEFAULT_STATUS_GLYPH = "◆"

fun NpcStatus.toStatusGlyph(): String =
    when (this) {
        NpcStatus.FRIEND -> "+"
        NpcStatus.ENEMY -> "×"
        NpcStatus.NEUTRAL -> "–"
    }

fun NpcLifeState.toStatusGlyph(): String =
    when (this) {
        NpcLifeState.ALIVE -> "✓"
        NpcLifeState.DEAD -> "†"
    }

fun QuestStatus.toStatusGlyph(): String =
    when (this) {
        QuestStatus.ACTIVE -> DEFAULT_STATUS_GLYPH
        QuestStatus.COMPLETED -> "✓"
        QuestStatus.FAILED -> "×"
    }
