package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.mention.domain.plainMentionText

// text fields show both values, the others (a status, a type) only say they differ
// a file that has nothing in a field never clears what the campaign has there
private fun text(
    field: RecordField,
    mine: String,
    theirs: String,
): FieldChange? =
    if (sameText(mine, theirs) || theirs.isBlank()) null else FieldChange(field, mine.trim(), theirs.trim())

// a mention points at an id of its own side, so two texts are told apart by what they say
private fun sameText(
    mine: String,
    theirs: String,
): Boolean = plainMentionText(mine).trim() == plainMentionText(theirs).trim()

private fun kind(
    field: RecordField,
    mine: String,
    theirs: String,
): FieldChange? = if (mine == theirs) null else FieldChange(field, mine, theirs, isText = false)

internal fun diffSpecies(
    file: FileRecord.Species,
    mine: com.lyrismet.incadent.db.species.SelectAll,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.name, file.name),
        text(RecordField.VULNERABILITIES, mine.vulnerabilities, file.vulnerabilities),
        text(RecordField.NOTES, mine.notes, file.notes),
        file.hitPoints?.let { text(RecordField.HIT_POINTS, mine.hit_points?.toString().orEmpty(), it.toString()) },
        kind(
            RecordField.CREATURE_TYPE,
            mine.custom_type_name ?: mine.creature_type.name,
            file.customType ?: file.creatureType.name,
        ),
    )

internal fun diffPlace(
    file: FileRecord.Place,
    mine: com.lyrismet.incadent.db.location.SelectAll,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.name, file.name),
        text(RecordField.DESCRIPTION, mine.description, file.description),
    )

internal fun diffNpc(
    file: FileRecord.Npc,
    mine: com.lyrismet.incadent.db.SelectAllWithLocations,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.name, file.name),
        text(RecordField.DESCRIPTION, mine.description, file.description),
        text(RecordField.RACE, mine.race, file.race),
        text(RecordField.NICKNAMES, mine.nicknames, file.nicknames),
        text(RecordField.VULNERABILITIES, mine.vulnerabilities, file.vulnerabilities),
        kind(RecordField.STATUS, mine.status.name, file.status.name),
        kind(RecordField.LIFE_STATE, mine.life_state.name, file.lifeState.name),
    )

internal fun diffQuest(
    file: FileRecord.Quest,
    mine: com.lyrismet.incadent.db.quest.SelectAll,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.title, file.title),
        text(RecordField.REWARD, mine.reward, file.reward),
        text(RecordField.DESCRIPTION, mine.description, file.description),
        kind(RecordField.STATUS, mine.status.name, file.status.name),
    )

/** vitals are left out, a friend copy never carries them, and the notes count only when the file carries them */
internal fun diffParty(
    file: FileRecord.Party,
    mine: com.lyrismet.incadent.db.partyMember.SelectAll,
    withNotes: Boolean,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.name, file.name),
        text(RecordField.CHARACTER_CLASS, mine.character_class, file.characterClass),
        text(RecordField.RACE, mine.race, file.race),
        file.level?.let { text(RecordField.LEVEL, mine.level?.toString().orEmpty(), it.toString()) },
        if (withNotes) text(RecordField.DESCRIPTION, mine.description, file.description) else null,
    )

internal fun diffItem(
    file: FileRecord.Item,
    mine: com.lyrismet.incadent.db.item.SelectAll,
): List<FieldChange> =
    listOfNotNull(
        text(RecordField.NAME, mine.name, file.name),
        text(RecordField.QUANTITY, mine.quantity.toString(), file.quantity.toString()),
        file.price?.let { text(RecordField.PRICE, mine.price?.toString().orEmpty(), it.toString()) },
        text(RecordField.NOTES, mine.notes, file.notes),
    )
