package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.characterSheet.SelectByMember
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_LEVEL
import com.lyrismet.incadent.feature.codex.domain.model.COIN_PURSE_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.FREE_CASTS_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.RESOURCE_MAX_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.model.SheetRanges
import com.lyrismet.incadent.feature.codex.domain.model.SpellRest

/** the sheet of a party member and its counters as file records, [partyNumber] is the number the records point at */
internal fun readSheetRecords(
    database: AppDatabase,
    memberId: Long,
    partyNumber: Long,
): List<FileRecord> {
    val sheet = database.characterSheetQueries.selectByMember(memberId).executeAsOneOrNull()
    val resources = database.characterResourceQueries.selectByMember(memberId).executeAsList()
    val spells = database.characterSpellQueries.selectByMember(memberId).executeAsList()
    return listOfNotNull(sheet?.toFileSheet(partyNumber)) +
        resources.map {
            FileRecord.Resource(
                partyNumber,
                it.name,
                it.max_value.toInt(),
                it.current_value.toInt(),
                it.slot_level?.toInt(),
                it.position.toInt(),
                it.origin_id,
            )
        } +
        spells.map {
            FileRecord.Spell(
                partyNumber,
                it.name,
                it.level.toInt(),
                it.school,
                it.casting_time,
                it.spell_range,
                it.verbal != 0L,
                it.somatic != 0L,
                it.has_material != 0L,
                it.material,
                it.duration,
                it.concentration != 0L,
                it.ritual != 0L,
                it.body,
                it.free_max.toInt(),
                it.free_left.toInt(),
                it.free_rest,
                it.position.toInt(),
                it.origin_id,
            )
        }
}

/** takes the sheet and the counters of a member away, the way before a file's are written over them */
internal fun clearSheet(
    database: AppDatabase,
    memberId: Long,
) {
    database.characterSheetQueries.deleteByMember(memberId)
    database.characterResourceQueries.deleteByMember(memberId)
    database.characterSpellQueries.deleteByMember(memberId)
}

/** the origin ids a sheet and a counter may still take, read once for a whole import */
internal class SheetOrigins(
    database: AppDatabase,
    campaignId: String,
) {
    val sheets = OriginSet(database.characterSheetQueries.selectOrigins(campaignId).executeAsList())
    val resources = OriginSet(database.characterResourceQueries.selectOrigins(campaignId).executeAsList())
    val spells = OriginSet(database.characterSpellQueries.selectOrigins(campaignId).executeAsList())
}

/** writes a sheet or a counter of a file to [memberId], every number pinned to the range the forms allow */
internal fun writeSheetRecord(
    database: AppDatabase,
    campaignId: String,
    memberId: Long,
    record: FileRecord,
    origins: SheetOrigins = SheetOrigins(database, campaignId),
) {
    when (record) {
        is FileRecord.Sheet -> writeSheet(database, campaignId, memberId, record, origins.sheets)
        is FileRecord.Resource -> writeResource(database, campaignId, memberId, record, origins.resources)
        is FileRecord.Spell -> writeSpell(database, campaignId, memberId, record, origins.spells)
        else -> Unit
    }
}

@Suppress("LongMethod")
private fun writeSheet(
    database: AppDatabase,
    campaignId: String,
    memberId: Long,
    record: FileRecord.Sheet,
    origins: OriginSet,
) {
    val queries = database.characterSheetQueries

    fun score(ability: Ability): Long? = record.abilities[ability]?.coerceIn(SheetRanges.ability)?.toLong()

    fun coin(amount: Int): Long = amount.coerceIn(COIN_PURSE_RANGE).toLong()
    queries.insert(
        party_member_id = memberId,
        ability_str = score(Ability.STRENGTH),
        ability_dex = score(Ability.DEXTERITY),
        ability_con = score(Ability.CONSTITUTION),
        ability_int = score(Ability.INTELLIGENCE),
        ability_wis = score(Ability.WISDOM),
        ability_cha = score(Ability.CHARISMA),
        saves = record.saves,
        skill_marks = record.skillMarks.filterValues { it in 1..2 },
        skill_adjustments =
            record.skillAdjustments
                .mapValues { it.value.coerceIn(SheetRanges.adjustment) }
                .filterValues { it != 0 },
        initiative_adjustment = record.initiativeAdjustment.coerceIn(SheetRanges.adjustment).toLong(),
        passive_perception_adjustment = record.passivePerceptionAdjustment.coerceIn(SheetRanges.adjustment).toLong(),
        spell_dc_adjustment = record.spellDcAdjustment.coerceIn(SheetRanges.adjustment).toLong(),
        spell_attack_adjustment = record.spellAttackAdjustment.coerceIn(SheetRanges.adjustment).toLong(),
        skill_notes = record.skillNotes,
        adjustment_notes = record.adjustmentNotes,
        spell_ability = record.spellAbility,
        speed = record.speed?.coerceIn(SheetRanges.speed)?.toLong(),
        inspiration = record.inspiration,
        spells_hidden = record.spellsHidden,
        background = record.background,
        alignment = record.alignment,
        personality = record.personality,
        languages = record.languages,
        features = record.features,
        notes = record.notes,
        coin_platinum = coin(record.platinum),
        coin_gold = coin(record.gold),
        coin_electrum = coin(record.electrum),
        coin_silver = coin(record.silver),
        coin_copper = coin(record.copper),
        campaign_id = campaignId,
        origin_id = origins.claim(record.originId),
    )
}

private fun writeResource(
    database: AppDatabase,
    campaignId: String,
    memberId: Long,
    record: FileRecord.Resource,
    origins: OriginSet,
) {
    val queries = database.characterResourceQueries
    val limit = record.max.coerceIn(RESOURCE_MAX_RANGE)
    queries.insert(
        memberId,
        record.name,
        limit.toLong(),
        record.current.coerceIn(0, limit).toLong(),
        record.slotLevel?.coerceIn(SPELL_LEVELS)?.toLong(),
        record.position.toLong(),
        campaignId,
        origins.claim(record.originId),
    )
}

private fun writeSpell(
    database: AppDatabase,
    campaignId: String,
    memberId: Long,
    record: FileRecord.Spell,
    origins: OriginSet,
) {
    val cap = record.freeMax.coerceIn(FREE_CASTS_RANGE)
    database.characterSpellQueries.insert(
        memberId,
        record.name,
        record.level.coerceIn(CANTRIP_LEVEL, SPELL_LEVELS.last).toLong(),
        record.school,
        record.castingTime,
        record.range,
        if (record.verbal) 1L else 0L,
        if (record.somatic) 1L else 0L,
        if (record.hasMaterial) 1L else 0L,
        record.material,
        record.duration,
        if (record.concentration) 1L else 0L,
        if (record.ritual) 1L else 0L,
        record.text,
        cap.toLong(),
        record.freeLeft.coerceIn(0, cap).toLong(),
        SpellRest.entries.firstOrNull { it.name == record.freeRest }?.name ?: SpellRest.LONG.name,
        record.position.toLong(),
        campaignId,
        origins.claim(record.originId),
    )
}

private fun SelectByMember.toFileSheet(partyNumber: Long) =
    FileRecord.Sheet(
        partyId = partyNumber,
        abilities =
            listOfNotNull(
                ability_str?.let { Ability.STRENGTH to it.toInt() },
                ability_dex?.let { Ability.DEXTERITY to it.toInt() },
                ability_con?.let { Ability.CONSTITUTION to it.toInt() },
                ability_int?.let { Ability.INTELLIGENCE to it.toInt() },
                ability_wis?.let { Ability.WISDOM to it.toInt() },
                ability_cha?.let { Ability.CHARISMA to it.toInt() },
            ).toMap(),
        saves = saves,
        skillMarks = skill_marks,
        skillAdjustments = skill_adjustments,
        initiativeAdjustment = initiative_adjustment.toInt(),
        passivePerceptionAdjustment = passive_perception_adjustment.toInt(),
        spellDcAdjustment = spell_dc_adjustment.toInt(),
        spellAttackAdjustment = spell_attack_adjustment.toInt(),
        skillNotes = skill_notes,
        adjustmentNotes = adjustment_notes,
        spellAbility = spell_ability,
        speed = speed?.toInt(),
        inspiration = inspiration,
        spellsHidden = spells_hidden,
        background = background,
        alignment = alignment,
        personality = personality,
        languages = languages,
        features = features,
        notes = notes,
        platinum = coin_platinum.toInt(),
        gold = coin_gold.toInt(),
        electrum = coin_electrum.toInt(),
        silver = coin_silver.toInt(),
        copper = coin_copper.toInt(),
        originId = origin_id,
    )
