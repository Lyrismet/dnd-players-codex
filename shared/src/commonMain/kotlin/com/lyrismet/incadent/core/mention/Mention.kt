package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

/** an npc/location/quest that session note text can @mention, carrying just enough state to color its chip */
sealed interface MentionEntity {
    val name: String

    data class NpcMention(
        val id: Long,
        override val name: String,
        val status: NpcStatus,
        val lifeState: NpcLifeState,
    ) : MentionEntity

    data class LocationMention(
        val id: Long,
        override val name: String,
    ) : MentionEntity

    data class QuestMention(
        val id: Long,
        override val name: String,
        val status: QuestStatus,
    ) : MentionEntity

    /** a character of the party - mentioned by name like an npc, but it has no relation or life state to colour it */
    data class PartyMention(
        val id: Long,
        override val name: String,
    ) : MentionEntity

    /** a record that is gone from the codex but still mentioned by notes - drawn as a "?" tag, never resolves */
    data class DeletedMention(
        override val name: String,
        val type: DeletedMentionType,
    ) : MentionEntity
}

fun mentionEntitiesFrom(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember> = emptyList(),
): List<MentionEntity> =
    npcs.map { MentionEntity.NpcMention(it.id, it.name, it.status, it.lifeState) } +
        parties.map { MentionEntity.PartyMention(it.id, it.name) } +
        locations.map { MentionEntity.LocationMention(it.id, it.name) } +
        quests.map { MentionEntity.QuestMention(it.id, it.title, it.status) }

/** the literal text typed after "@" for [entity] - quests are always mentioned as "<prefix><title>" */
fun mentionKey(
    entity: MentionEntity,
    questPrefix: String,
): String =
    when (entity) {
        is MentionEntity.QuestMention -> questPrefix + entity.name
        is MentionEntity.DeletedMention ->
            if (entity.type == DeletedMentionType.QUEST) questPrefix + entity.name else entity.name
        is MentionEntity.NpcMention, is MentionEntity.LocationMention, is MentionEntity.PartyMention -> entity.name
    }

data class MentionCandidate(
    val key: String,
    val entity: MentionEntity,
)

// longest key first so "@Кассиан Вейл" beats "@Кассиан", a deleted record yields to a live one with its key
fun mentionCandidates(
    entities: List<MentionEntity>,
    questPrefix: String,
    deleted: List<DeletedMention> = emptyList(),
): List<MentionCandidate> {
    val live = entities.map { MentionCandidate(mentionKey(it, questPrefix), it) }
    val liveKeys = live.mapTo(HashSet()) { it.key }
    val gone =
        deleted
            .map { MentionEntity.DeletedMention(it.name, it.type) }
            .map { MentionCandidate(mentionKey(it, questPrefix), it) }
            .filter { it.key !in liveKeys }
            .distinctBy { it.key }
    return (live + gone).sortedByDescending { it.key.length }
}

sealed interface MentionSegment {
    data class Text(
        val text: String,
    ) : MentionSegment

    data class Mention(
        val entity: MentionEntity,
    ) : MentionSegment

    /** a codex name written as a whole word without "@" - only produced when auto matching is asked for */
    data class AutoMatch(
        val text: String,
        val entity: MentionEntity,
    ) : MentionSegment
}

/** splits [text] on "@" + a candidate key (longest first), [autoMatch] also finds bare whole-word names */
fun parseMentions(
    text: String,
    candidates: List<MentionCandidate>,
    autoMatch: Boolean = false,
): List<MentionSegment> =
    when {
        text.isEmpty() -> emptyList()
        candidates.isEmpty() || ('@' !in text && !autoMatch) -> listOf(MentionSegment.Text(text))
        else -> splitOnMentions(text, candidates, if (autoMatch) autoNamesOf(candidates) else emptyList())
    }

private fun splitOnMentions(
    text: String,
    candidates: List<MentionCandidate>,
    autoNames: List<AutoName>,
): List<MentionSegment> {
    val segments = mutableListOf<MentionSegment>()
    val buffer = StringBuilder()
    val folded = if (autoNames.isEmpty()) "" else text.foldPerChar()

    fun flush() {
        if (buffer.isNotEmpty()) {
            segments += MentionSegment.Text(buffer.toString())
            buffer.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        val mention = if (text[i] == '@') candidates.firstOrNull { text.startsWith(it.key, i + 1) } else null
        val auto = if (mention == null) autoNames.matchAt(text, folded, i) else null
        when {
            mention != null -> {
                flush()
                segments += MentionSegment.Mention(mention.entity)
                i += 1 + mention.key.length
            }
            auto != null -> {
                flush()
                segments += MentionSegment.AutoMatch(text.substring(i, i + auto.folded.length), auto.entity)
                i += auto.folded.length
            }
            else -> {
                buffer.append(text[i])
                i++
            }
        }
    }
    flush()
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
        is MentionEntity.PartyMention -> "party:$id"
        is MentionEntity.DeletedMention -> "deleted:${type.name}:$name"
    }
