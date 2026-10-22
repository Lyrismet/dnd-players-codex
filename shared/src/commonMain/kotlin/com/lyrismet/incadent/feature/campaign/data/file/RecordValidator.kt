package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.codex.domain.model.SPECIES_HIT_POINTS_RANGE
import com.lyrismet.incadent.feature.mention.domain.mentionRefsIn

// far above what the forms allow (see TextLimits), so a real export always fits and a stuffed field never does
private const val MAX_SHORT_CHARS = 200
private const val MAX_NICKNAMES_CHARS = 1_000
private const val MAX_LONG_CHARS = 20_000
private const val MAX_LINKS = 10_000
private const val MAX_HP_WITHOUT_MAX = 999

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
        checkLengths(record)
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

    // a file is never shorter than a form allows, but it can be stuffed to bloat the database
    private fun checkLengths(record: FileRecord) {
        must(
            when (record) {
                is FileRecord.Species -> record.fits()
                is FileRecord.Place -> record.fits()
                is FileRecord.Npc -> record.fits()
                is FileRecord.Quest -> record.fits()
                is FileRecord.Party -> record.fits()
                is FileRecord.Tag -> fits(MAX_SHORT_CHARS, record.name)
                is FileRecord.Phrase -> fits(MAX_LONG_CHARS, record.text)
                is FileRecord.Session -> record.fits()
                is FileRecord.Entry -> fits(MAX_LONG_CHARS, record.body) && record.body.hasReadableTokens()
                is FileRecord.Photo -> true
            },
        )
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

private fun fits(
    limit: Int,
    vararg texts: String,
): Boolean = texts.all { it.length <= limit }

private fun FileRecord.Species.fits() =
    fits(MAX_SHORT_CHARS, name) &&
        fits(MAX_LONG_CHARS, notes, damage) &&
        fits(MAX_NICKNAMES_CHARS, nicknames) &&
        (hitPoints == null || hitPoints in SPECIES_HIT_POINTS_RANGE)

private fun FileRecord.Place.fits() = fits(MAX_SHORT_CHARS, name) && fits(MAX_LONG_CHARS, description)

private fun FileRecord.Npc.fits() =
    fits(MAX_SHORT_CHARS, name, race) &&
        fits(MAX_LONG_CHARS, description) &&
        fits(MAX_NICKNAMES_CHARS, nicknames) &&
        placeIds.size <= MAX_LINKS

private fun FileRecord.Quest.fits() = fits(MAX_SHORT_CHARS, title, reward) && fits(MAX_LONG_CHARS, description)

private fun FileRecord.Party.fits() =
    fits(MAX_SHORT_CHARS, name, characterClass, race, playerName) &&
        fits(MAX_LONG_CHARS, description) &&
        (level == null || level in PartyStatRanges.level) &&
        (hpMax == null || hpMax in PartyStatRanges.hpMax) &&
        (hpCurrent == null || hpCurrent in 0..(hpMax ?: MAX_HP_WITHOUT_MAX))

private fun FileRecord.Session.fits() =
    fits(MAX_SHORT_CHARS, title) && tags.size <= MAX_LINKS && tags.all { it.length <= MAX_SHORT_CHARS }

// a token with an id beyond a Long makes the mention parser throw, which would not read as a bad file
private fun String.hasReadableTokens(): Boolean = runCatching { mentionRefsIn(this) }.isSuccess
