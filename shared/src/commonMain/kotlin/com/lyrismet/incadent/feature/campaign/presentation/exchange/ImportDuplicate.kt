package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.feature.campaign.domain.exchange.nameKey
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope

/** a record of the file that has the same name and kind as one already in the campaign it goes into */
data class ImportDuplicate(
    val name: String,
    val kind: ExchangeKind,
)

/** the records of [scope] (listed in [file]) that [existing] already has under the same name, case and edges ignored */
fun findDuplicates(
    scope: RecordScope,
    file: ExchangeOptions,
    existing: ExchangeOptions,
): List<ImportDuplicate> {
    fun matches(
        kind: ExchangeKind,
        ids: Set<Long>,
        incoming: List<ExchangeItem>,
        there: List<ExchangeItem>,
    ): List<ImportDuplicate> {
        val names = there.map { it.name.nameKey() }.toSet()
        return incoming.filter { it.id in ids && it.name.nameKey() in names }.map { ImportDuplicate(it.name, kind) }
    }
    return matches(ExchangeKind.PLACE, scope.places, file.places, existing.places) +
        matches(ExchangeKind.NPC, scope.npcs, file.npcs, existing.npcs) +
        matches(ExchangeKind.QUEST, scope.quests, file.quests, existing.quests) +
        matches(ExchangeKind.SPECIES, scope.species, file.species, existing.species) +
        matches(ExchangeKind.PARTY, scope.party, file.party, existing.party)
}
