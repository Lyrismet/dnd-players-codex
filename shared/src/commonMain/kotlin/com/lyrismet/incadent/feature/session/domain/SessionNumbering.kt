package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.session.domain.model.SessionNote

// the current session is the newest whatever its date says, the diary shows it on top
private fun List<SessionNote>.oldestFirst(): List<SessionNote> =
    sortedWith(
        compareBy({
            it.isLive
        }, { it.sessionDate }),
    )

/** each session's 1-based position by chronological order (oldest = 1), independent of list sort order */
fun List<SessionNote>.chronologicalIndex(): Map<Long, Int> =
    oldestFirst().mapIndexed { index, note -> note.id to (index + 1) }.toMap()

/** the id of the session that comes right after each one by date, the latest session has none */
fun List<SessionNote>.nextSessionIds(): Map<Long, Long> =
    oldestFirst().zipWithNext().associate { (earlier, later) -> earlier.id to later.id }

/** each session's roman-numeral position by chronological order (oldest = I) - see [chronologicalIndex] */
fun List<SessionNote>.chronologicalNumberLabels(): Map<Long, String> =
    chronologicalIndex().mapValues { (_, index) -> index.toRomanNumeral() }

/** the session labels in the style the user picked in Settings - see [chronologicalNumberLabels] */
fun List<SessionNote>.sessionNumberLabels(numbering: SessionNumbering): Map<Long, String> =
    chronologicalIndex().mapValues { (_, index) -> sessionNumberLabel(index, numbering) }

/** the label of the session in place [position] (1 is the oldest) in the style the user picked in Settings */
fun sessionNumberLabel(
    position: Int,
    numbering: SessionNumbering,
): String =
    when (numbering) {
        SessionNumbering.ROMAN -> position.toRomanNumeral()
        SessionNumbering.ARABIC -> position.toString()
    }
