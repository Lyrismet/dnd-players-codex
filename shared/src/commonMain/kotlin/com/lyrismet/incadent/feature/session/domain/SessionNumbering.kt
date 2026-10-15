package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.session.domain.model.SessionNote

/** each session's 1-based position by chronological order (oldest = 1), independent of list sort order */
fun List<SessionNote>.chronologicalIndex(): Map<Long, Int> =
    sortedBy { it.sessionDate }.mapIndexed { index, note -> note.id to (index + 1) }.toMap()

/** each session's roman-numeral position by chronological order (oldest = I) - see [chronologicalIndex] */
fun List<SessionNote>.chronologicalNumberLabels(): Map<Long, String> =
    chronologicalIndex().mapValues { (_, index) -> index.toRomanNumeral() }

/** the session labels in the style the user picked in Settings - see [chronologicalNumberLabels] */
fun List<SessionNote>.sessionNumberLabels(numbering: SessionNumbering): Map<Long, String> =
    when (numbering) {
        SessionNumbering.ROMAN -> chronologicalNumberLabels()
        SessionNumbering.ARABIC -> chronologicalIndex().mapValues { (_, index) -> index.toString() }
    }
