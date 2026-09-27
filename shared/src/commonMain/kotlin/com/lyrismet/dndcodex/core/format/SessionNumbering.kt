package com.lyrismet.dndcodex.core.format

import com.lyrismet.dndcodex.domain.model.SessionNote

/** each session's 1-based position by chronological order (oldest = 1), independent of list sort order */
fun List<SessionNote>.chronologicalIndex(): Map<Long, Int> =
    sortedBy { it.sessionDate }.mapIndexed { index, note -> note.id to (index + 1) }.toMap()

/** each session's roman-numeral position by chronological order (oldest = I) - see [chronologicalIndex] */
fun List<SessionNote>.chronologicalNumberLabels(): Map<Long, String> =
    chronologicalIndex().mapValues { (_, index) -> index.toRomanNumeral() }
