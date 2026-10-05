package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

/** one titled section of a list - a null [title] is the ungrouped "as a list" view */
data class CodexGroup<T>(
    val title: String?,
    val items: List<T>,
)

/** an entity inside a group, with the subtitle the grouping swaps in - null keeps the entity's default subtitle */
data class CodexGroupItem<T>(
    val entity: T,
    val subtitle: String?,
)

fun <T, R> CodexGroup<T>.map(transform: (T) -> R): CodexGroup<R> = CodexGroup(title, items.map(transform))

/** the words the section titles are built from, resolved from resources by the presenter */
data class CodexGroupLabels(
    val noRegionForQuest: String,
    val noPlace: String,
    val noFaction: String,
    val noGiver: String,
    val giverPrefix: String,
    val noType: String,
    val npcStatus: Map<NpcStatus, String>,
    val questStatus: Map<QuestStatus, String>,
)

fun groupNpcs(
    npcs: List<Npc>,
    locations: List<Location>,
    by: NpcGroupBy,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(npcs, emptyList(), locations),
): List<CodexGroup<CodexGroupItem<Npc>>> {
    val rows =
        sortNpcs(npcs).map { npc ->
            CodexGroupItem(npc, subtitle = if (by == NpcGroupBy.REGION) placeLine(npc, locations) else null)
        }
    return when (by) {
        NpcGroupBy.NONE -> rows.asUngrouped()
        NpcGroupBy.REGION ->
            rows.toSections { regions.regionOfNpc(it.entity).toSectionKey(labels.noPlace) }
        NpcGroupBy.RELATION -> rows.toSections { it.entity.status.toSectionKey(labels.npcStatus) }
        NpcGroupBy.FACTION -> rows.toSections { it.entity.faction.toSectionKey(labels.noFaction) }
    }
}

@Suppress("LongParameterList")
fun groupQuests(
    quests: List<Quest>,
    npcs: List<Npc>,
    locations: List<Location>,
    by: QuestGroupBy,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(npcs, quests, locations),
): List<CodexGroup<Quest>> {
    val rows = sortQuests(quests)
    return when (by) {
        QuestGroupBy.NONE -> rows.asUngrouped()
        QuestGroupBy.REGION ->
            rows.toSections { regions.regionOfQuest(it).toSectionKey(labels.noRegionForQuest) }
        QuestGroupBy.STATUS -> rows.toSections { it.status.toSectionKey(labels.questStatus) }
        QuestGroupBy.GIVER -> rows.toSections { giverKey(it, npcs, labels) }
    }
}

fun groupLocations(
    locations: List<Location>,
    by: LocationGroupBy,
    allLocations: List<Location>,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(emptyList(), emptyList(), allLocations),
): List<CodexGroup<CodexGroupItem<Location>>> {
    val rows =
        sortLocations(locations).map { place ->
            CodexGroupItem(place, locationSubtitle(place, by, regions))
        }
    return when (by) {
        LocationGroupBy.NONE -> rows.asUngrouped()
        LocationGroupBy.REGION ->
            rows.toSections { regions.regionOfLocation(it.entity).toSectionKey(labels.noPlace) }
        LocationGroupBy.TYPE -> rows.toSections { it.entity.type.toSectionKey(labels.noType) }
    }
}

private fun locationSubtitle(
    place: Location,
    by: LocationGroupBy,
    regions: CodexRegions,
): String? = if (by == LocationGroupBy.REGION) regionSubtitle(place, regions.regionOfLocation(place)) else null

private fun <T> List<T>.asUngrouped(): List<CodexGroup<T>> =
    if (isEmpty()) emptyList() else listOf(CodexGroup(null, this))

// keeps the incoming order inside each section, orders the sections by key with the "no value" one last
private fun <T> List<T>.toSections(keyOf: (T) -> SectionKey): List<CodexGroup<T>> {
    val sections = groupBy(keyOf).toList()
    val ordered =
        sections.sortedWith(
            compareBy<Pair<SectionKey, List<T>>>(
                { it.first.order == null },
                { it.first.order },
                { it.first.title },
            ),
        )
    return ordered.map { (key, items) -> CodexGroup(key.title, items) }
}
