package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

/** an npc/location/quest that session note text can @mention, carrying just enough state to color its chip */
sealed interface MentionEntity {
    val id: Long
    val name: String

    data class NpcMention(
        override val id: Long,
        override val name: String,
        val status: NpcStatus,
    ) : MentionEntity

    data class LocationMention(
        override val id: Long,
        override val name: String,
    ) : MentionEntity

    data class QuestMention(
        override val id: Long,
        override val name: String,
        val status: QuestStatus,
    ) : MentionEntity
}

fun mentionEntitiesFrom(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
): List<MentionEntity> =
    npcs.map { MentionEntity.NpcMention(it.id, it.name, it.status) } +
        locations.map { MentionEntity.LocationMention(it.id, it.name) } +
        quests.map { MentionEntity.QuestMention(it.id, it.title, it.status) }

/** the literal text typed after "@" for [entity] - quests are always mentioned as "<prefix><title>" */
fun mentionKey(
    entity: MentionEntity,
    questPrefix: String,
): String = if (entity is MentionEntity.QuestMention) questPrefix + entity.name else entity.name

data class MentionCandidate(
    val key: String,
    val entity: MentionEntity,
)

fun mentionCandidates(
    entities: List<MentionEntity>,
    questPrefix: String,
): List<MentionCandidate> = entities.map { MentionCandidate(mentionKey(it, questPrefix), it) }

sealed interface MentionSegment {
    data class Text(
        val text: String,
    ) : MentionSegment

    data class Mention(
        val entity: MentionEntity,
    ) : MentionSegment
}

/** splits [text] on "@" + a known candidate key, longest key first so "@Кассиан Вейл" beats "@Кассиан" */
fun parseMentions(
    text: String,
    candidates: List<MentionCandidate>,
): List<MentionSegment> =
    when {
        text.isEmpty() -> emptyList()
        candidates.isEmpty() || '@' !in text -> listOf(MentionSegment.Text(text))
        else -> splitOnMentions(text, candidates)
    }

private fun splitOnMentions(
    text: String,
    candidates: List<MentionCandidate>,
): List<MentionSegment> {
    val sortedByLongestKey = candidates.sortedByDescending { it.key.length }
    val segments = mutableListOf<MentionSegment>()
    val buffer = StringBuilder()
    var i = 0
    while (i < text.length) {
        val match = if (text[i] == '@') sortedByLongestKey.firstOrNull { text.startsWith(it.key, i + 1) } else null
        if (match != null) {
            if (buffer.isNotEmpty()) {
                segments += MentionSegment.Text(buffer.toString())
                buffer.clear()
            }
            segments += MentionSegment.Mention(match.entity)
            i += 1 + match.key.length
        } else {
            buffer.append(text[i])
            i++
        }
    }
    if (buffer.isNotEmpty()) segments += MentionSegment.Text(buffer.toString())
    return segments
}

// a space ends the mention token - also what closes the popover once insertMention's own trailing space lands
private val TRAILING_MENTION_REGEX = Regex("@([^@\\s]{0,24})$")

/** the in-progress "@query" at the end of [draft], if the caret is mid-mention - drives the suggestion popover */
fun trailingMentionQuery(draft: String): String? = TRAILING_MENTION_REGEX.find(draft)?.groupValues?.get(1)

fun matchingMentionEntities(
    entities: List<MentionEntity>,
    rawQuery: String,
    questPrefix: String,
    limit: Int = 5,
): List<MentionEntity> {
    val prefixLower = questPrefix.trim().lowercase()
    var query = rawQuery.trim().lowercase()
    if (prefixLower.isNotEmpty() && query.startsWith(prefixLower)) {
        query = query.removePrefix(prefixLower).trimStart()
    }
    return entities
        .filter { entity ->
            val name = entity.name.lowercase()
            name.startsWith(query) || (query.length > 1 && name.contains(query))
        }.take(limit)
}

/** replaces the trailing "@query" in [draft] with the full mention text for [candidateKey], plus a trailing space */
fun insertMention(
    draft: String,
    candidateKey: String,
): String = TRAILING_MENTION_REGEX.replace(draft) { "@$candidateKey " }

/** every distinct entity mentioned across [bodies], in first-seen order - the caller decides whether to cap it */
fun mentionsIn(
    bodies: List<String>,
    candidates: List<MentionCandidate>,
): List<MentionEntity> =
    bodies
        .flatMap { parseMentions(it, candidates) }
        .filterIsInstance<MentionSegment.Mention>()
        .map { it.entity }
        .distinctBy { it.dedupeKey() }

fun MentionEntity.dedupeKey(): String =
    when (this) {
        is MentionEntity.NpcMention -> "npc:$id"
        is MentionEntity.LocationMention -> "location:$id"
        is MentionEntity.QuestMention -> "quest:$id"
    }
