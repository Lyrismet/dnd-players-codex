package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.codexgroup.CodexGroup
import com.lyrismet.incadent.core.codexgroup.CodexGroupBy
import com.lyrismet.incadent.core.codexgroup.CodexGroupLabels
import com.lyrismet.incadent.core.codexgroup.CodexGroupingSelection
import com.lyrismet.incadent.core.codexgroup.LocationGroupBy
import com.lyrismet.incadent.core.codexgroup.NpcGroupBy
import com.lyrismet.incadent.core.codexgroup.QuestGroupBy
import com.lyrismet.incadent.core.codexgroup.groupLocations
import com.lyrismet.incadent.core.codexgroup.groupNpcs
import com.lyrismet.incadent.core.codexgroup.groupQuests
import com.lyrismet.incadent.core.codexgroup.map
import com.lyrismet.incadent.domain.model.NpcLifeState

/** the chip labels of each tab's group-by row */
internal class CodexGroupOptionLabels(
    val npc: Map<NpcGroupBy, String>,
    val quest: Map<QuestGroupBy, String>,
    val place: Map<LocationGroupBy, String>,
)

/** every label the grouped lists need - section titles, chip labels and the npc life badge */
internal class CodexGroupedLabels(
    val titles: CodexGroupLabels,
    val options: CodexGroupOptionLabels,
    val npcLife: Map<NpcLifeState, String>,
)

/** the grouped, sorted lists the tabs render, plus the group row shown above them */
internal class CodexGroupedLists(
    val npcs: List<CodexGroup<NpcCodexItem>>,
    val quests: List<CodexGroup<QuestCodexItem>>,
    val locations: List<CodexGroup<LocationCodexItem>>,
    val groupBy: CodexGroupBy?,
    val groupOptions: List<CodexFilterOption<CodexGroupBy>>,
)

/** the group choice of the active tab - null on the tabs that have no group row */
internal fun codexGroupBy(
    tab: CodexTab,
    selection: CodexGroupingSelection,
): CodexGroupBy? =
    when (tab) {
        CodexTab.NPC -> CodexGroupBy.Npc(selection.npc)
        CodexTab.QUEST -> CodexGroupBy.Quest(selection.quest)
        CodexTab.LOCATION -> CodexGroupBy.Place(selection.place)
        CodexTab.ALL, CodexTab.PARTY -> null
    }

internal fun codexGroupOptions(
    tab: CodexTab,
    labels: CodexGroupOptionLabels,
): List<CodexFilterOption<CodexGroupBy>> =
    when (tab) {
        CodexTab.NPC -> labels.npc.map { (by, label) -> groupOption(CodexGroupBy.Npc(by), label) }
        CodexTab.QUEST -> labels.quest.map { (by, label) -> groupOption(CodexGroupBy.Quest(by), label) }
        CodexTab.LOCATION -> labels.place.map { (by, label) -> groupOption(CodexGroupBy.Place(by), label) }
        CodexTab.ALL, CodexTab.PARTY -> emptyList()
    }

private fun groupOption(
    choice: CodexGroupBy,
    label: String,
): CodexFilterOption<CodexGroupBy> = CodexFilterOption(value = choice, label = label)

/** groups and sorts every list from the search results - a tab only groups while it is the active one */
internal fun codexGroupedLists(
    search: CodexSearchResults,
    records: CodexRecords,
    tab: CodexTab,
    selection: CodexGroupingSelection,
    labels: CodexGroupedLabels,
): CodexGroupedLists {
    val npcBy = if (tab == CodexTab.NPC) selection.npc else NpcGroupBy.NONE
    val questBy = if (tab == CodexTab.QUEST) selection.quest else QuestGroupBy.NONE
    val placeBy = if (tab == CodexTab.LOCATION) selection.place else LocationGroupBy.NONE
    return CodexGroupedLists(
        npcs =
            groupNpcs(search.searchedNpcs, records.locations, npcBy, labels.titles).map { group ->
                group.map { row -> row.entity.toCodexItem(labels.titles.npcStatus, labels.npcLife, row.subtitle) }
            },
        quests =
            groupQuests(search.searchedQuests, records.npcs, records.locations, questBy, labels.titles).map { group ->
                group.map { quest -> quest.toCodexItem(search.npcsById, labels.titles.questStatus) }
            },
        locations =
            groupLocations(search.searchedLocations, placeBy, records.locations, labels.titles).map { group ->
                group.map { row -> row.entity.toCodexItem(row.subtitle) }
            },
        groupBy = codexGroupBy(tab, selection),
        groupOptions = codexGroupOptions(tab, labels.options),
    )
}
