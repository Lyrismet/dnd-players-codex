package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.identity.newOriginId

/** the origin ids of one kind that are taken in the target campaign, a file's id is kept only while it is free */
internal class OriginSet(
    taken: Collection<String> = emptyList(),
) {
    private val taken = taken.toMutableSet()

    /** the id a new row gets, the file's own when it has one and nobody holds it, else a fresh one */
    fun claim(fromFile: String?): String {
        val id = fromFile?.takeIf { it.isNotBlank() && it !in taken } ?: newOriginId()
        taken += id
        return id
    }
}
