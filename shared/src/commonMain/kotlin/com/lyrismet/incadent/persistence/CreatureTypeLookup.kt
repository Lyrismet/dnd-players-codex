package com.lyrismet.incadent.persistence

import com.lyrismet.incadent.db.AppDatabase

/**
 * the id of the creature type [name] in [campaignId], written in lower case like every own type and made there when
 * the campaign has none of that name yet. A blank or missing name has no type.
 */
fun AppDatabase.creatureTypeIdIn(
    campaignId: String,
    name: String?,
): Long? {
    val typed = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    return typed?.let {
        creatureTypeQueries.selectByName(campaignId, it).executeAsOneOrNull()?.id
            ?: run {
                creatureTypeQueries.insert(it, campaignId)
                creatureTypeQueries.lastInsertRowId().executeAsOne()
            }
    }
}
