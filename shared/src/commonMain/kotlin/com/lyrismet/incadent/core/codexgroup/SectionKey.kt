package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

/** a section key - [order] null means the "no value" section, which always comes last */
internal data class SectionKey(
    val title: String,
    val order: String?,
)

internal fun giverKey(
    quest: Quest,
    npcs: List<Npc>,
    labels: CodexGroupLabels,
): SectionKey {
    val giver = npcs.byId(quest.givenByNpcId) ?: return SectionKey(labels.noGiver.uppercase(), order = null)
    return SectionKey(
        title = "${labels.giverPrefix} ${giver.name}".uppercase(),
        order = giver.name.collationKey(),
    )
}

internal fun String?.toSectionKey(noValueTitle: String): SectionKey {
    val value = valueOrNull() ?: return SectionKey(noValueTitle.uppercase(), order = null)
    return SectionKey(value.uppercase(), order = value.collationKey())
}

internal fun NpcStatus.toSectionKey(labels: Map<NpcStatus, String>): SectionKey =
    SectionKey(labels.getValue(this).uppercase(), order = ordinal.toString())

internal fun QuestStatus.toSectionKey(labels: Map<QuestStatus, String>): SectionKey =
    SectionKey(labels.getValue(this).uppercase(), order = ordinal.toString())
