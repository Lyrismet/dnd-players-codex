package com.lyrismet.incadent.feature.campaign.domain.exchange

import kotlinx.datetime.LocalDate

/** a session counts as the same one when the day and the title agree, the database has no session numbers */
fun sessionKey(
    day: LocalDate,
    title: String,
): String = "$day|${title.nameKey()}"

/** how two notes lists of one session line up: the notes both hold, and the ones each has alone */
data class NoteOverlap(
    val mine: Int,
    val theirs: Int,
    val common: Int,
) {
    /** the notes the session holds after the merge */
    val merged: Int get() = mine + theirs - common
}

/** [mine] and [theirs] are the note origin ids of two sessions, a note without one never counts as common */
fun noteOverlap(
    mine: List<String>,
    theirs: List<String>,
): NoteOverlap {
    val known = mine.filter { it.isNotBlank() }.toSet()
    val common = theirs.count { it.isNotBlank() && it in known }
    return NoteOverlap(mine.size, theirs.size, common)
}
