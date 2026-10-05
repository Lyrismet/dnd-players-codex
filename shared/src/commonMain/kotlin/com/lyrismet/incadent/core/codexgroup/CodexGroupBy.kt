package com.lyrismet.incadent.core.codexgroup

enum class NpcGroupBy {
    REGION,
    RELATION,
    FACTION,
    NONE,
}

enum class QuestGroupBy {
    REGION,
    STATUS,
    GIVER,
    NONE,
}

enum class LocationGroupBy {
    REGION,
    TYPE,
    NONE,
}

/** one group-by choice, tagged with the tab it belongs to */
sealed interface CodexGroupBy {
    data class Npc(
        val by: NpcGroupBy,
    ) : CodexGroupBy

    data class Quest(
        val by: QuestGroupBy,
    ) : CodexGroupBy

    data class Place(
        val by: LocationGroupBy,
    ) : CodexGroupBy
}

/** the group-by choice each grouped tab remembers, every tab starts on its region grouping */
data class CodexGroupingSelection(
    val npc: NpcGroupBy = NpcGroupBy.REGION,
    val quest: QuestGroupBy = QuestGroupBy.REGION,
    val place: LocationGroupBy = LocationGroupBy.REGION,
) {
    fun select(choice: CodexGroupBy): CodexGroupingSelection =
        when (choice) {
            is CodexGroupBy.Npc -> copy(npc = choice.by)
            is CodexGroupBy.Quest -> copy(quest = choice.by)
            is CodexGroupBy.Place -> copy(place = choice.by)
        }
}
