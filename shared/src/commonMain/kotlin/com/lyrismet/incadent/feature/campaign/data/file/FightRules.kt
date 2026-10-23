package com.lyrismet.incadent.feature.campaign.data.file

/** the rules of the fight records, a fighter points only at records and fights seen above it */
internal class FightRules(
    private val species: Set<Long>,
    private val npcs: Set<Long>,
    private val party: Set<Long>,
    private val sessions: Set<Long>,
) {
    private val combats = mutableSetOf<Long>()
    private val fighters = mutableSetOf<Pair<Long, Long>>()

    /** false when the record breaks a rule */
    fun accept(record: FightRecord) =
        when (record) {
            is FileRecord.Combat -> record.sessionId in sessions && combats.add(record.id)
            is FileRecord.Fighter -> fighterFits(record)
            is FileRecord.Blow ->
                (record.combatId to record.targetId) in fighters &&
                    (record.sourceId == null || (record.combatId to record.sourceId) in fighters)
            is FileRecord.Observation -> record.combatId in combats && record.speciesId in species
        }

    private fun fighterFits(record: FileRecord.Fighter) =
        record.combatId in combats &&
            (record.npcId == null || record.npcId in npcs) &&
            (record.speciesId == null || record.speciesId in species) &&
            (record.partyId == null || record.partyId in party) &&
            fighters.add(record.combatId to record.id)

    fun fits(record: FightRecord) =
        when (record) {
            is FileRecord.Fighter -> record.name.length <= MAX_SHORT_CHARS && record.note.length <= MAX_LONG_CHARS
            is FileRecord.Blow -> (record.detail?.length ?: 0) <= MAX_SHORT_CHARS
            is FileRecord.Combat, is FileRecord.Observation -> true
        }
}
