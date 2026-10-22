package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException

/** checks that records arrive in the file's fixed order and only point at ones already seen, nothing is trusted */
class RecordValidator {
    private var rank = 0
    private val species = mutableSetOf<Long>()
    private val places = mutableSetOf<Long>()
    private val npcs = mutableSetOf<Long>()
    private val quests = mutableSetOf<Long>()
    private val party = mutableSetOf<Long>()
    private val sessions = mutableSetOf<Long>()

    fun accept(record: FileRecord) {
        val next = record.rank()
        must(next >= rank)
        rank = next
        when (record) {
            is FileRecord.Species -> must(species.add(record.id))
            is FileRecord.Place -> {
                must(record.parentId == null || record.parentId in places)
                must(places.add(record.id))
            }
            is FileRecord.Npc -> {
                must(record.speciesId == null || record.speciesId in species)
                must(record.placeIds.all { it in places })
                must(npcs.add(record.id))
            }
            is FileRecord.Quest -> {
                must(record.giverNpcId == null || record.giverNpcId in npcs)
                must(record.placeId == null || record.placeId in places)
                must(quests.add(record.id))
            }
            is FileRecord.Party -> must(party.add(record.id))
            is FileRecord.Session -> must(sessions.add(record.id))
            is FileRecord.Entry -> must(record.sessionId in sessions)
            is FileRecord.Photo -> must(record.ownerId in owners(record.owner))
            is FileRecord.Tag, is FileRecord.Phrase -> Unit
        }
    }

    private fun owners(owner: PhotoOwner): Set<Long> =
        when (owner) {
            PhotoOwner.SPECIES -> species
            PhotoOwner.PLACE -> places
            PhotoOwner.NPC -> npcs
            PhotoOwner.PARTY -> party
        }

    private fun must(holds: Boolean) {
        if (!holds) throw ImportFailedException(ImportError.Malformed)
    }
}

// a place's parent, an npc's species and places, a quest's npc and place all sit in sections above them
private enum class Section { SPECIES, PLACE, NPC, QUEST, PARTY, TAG, PHRASE, SESSION, ENTRY, PHOTO }

private fun FileRecord.rank(): Int =
    when (this) {
        is FileRecord.Species -> Section.SPECIES
        is FileRecord.Place -> Section.PLACE
        is FileRecord.Npc -> Section.NPC
        is FileRecord.Quest -> Section.QUEST
        is FileRecord.Party -> Section.PARTY
        is FileRecord.Tag -> Section.TAG
        is FileRecord.Phrase -> Section.PHRASE
        is FileRecord.Session -> Section.SESSION
        is FileRecord.Entry -> Section.ENTRY
        is FileRecord.Photo -> Section.PHOTO
    }.ordinal
