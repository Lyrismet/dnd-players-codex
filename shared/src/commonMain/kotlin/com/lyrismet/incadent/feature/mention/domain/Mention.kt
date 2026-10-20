package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus

/** one record a note can mention, told apart by kind and id so records sharing a name stay separate */
data class MentionRef(
    val type: EntityKind,
    val id: Long,
)

/** an npc/location/quest that session note text can @mention, carrying just enough state to color its chip */
sealed interface MentionEntity {
    val name: String
    val ref: MentionRef

    data class NpcMention(
        val id: Long,
        override val name: String,
        val status: NpcStatus,
        val lifeState: NpcLifeState,
    ) : MentionEntity {
        override val ref: MentionRef get() = MentionRef(EntityKind.NPC, id)
    }

    data class LocationMention(
        val id: Long,
        override val name: String,
    ) : MentionEntity {
        override val ref: MentionRef get() = MentionRef(EntityKind.LOCATION, id)
    }

    data class QuestMention(
        val id: Long,
        override val name: String,
        val status: QuestStatus,
    ) : MentionEntity {
        override val ref: MentionRef get() = MentionRef(EntityKind.QUEST, id)
    }

    /** a character of the party - mentioned by name like an npc, but it has no relation or life state to colour it */
    data class PartyMention(
        val id: Long,
        override val name: String,
    ) : MentionEntity {
        override val ref: MentionRef get() = MentionRef(EntityKind.PARTY, id)
    }

    /** a record that is gone from the codex but still mentioned by notes - drawn as a "?" tag, named by its token */
    data class DeletedMention(
        val id: Long,
        override val name: String,
        val type: EntityKind,
    ) : MentionEntity {
        override val ref: MentionRef get() = MentionRef(type, id)
    }
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

/** every record a note can mention right now, looked up by [MentionRef] - names are never used to find a record */
class MentionIndex(
    val entities: List<MentionEntity>,
) {
    private val byRef = entities.associateBy { it.ref }

    fun has(ref: MentionRef): Boolean = ref in byRef

    /** the live record [ref] points at, or the "?" stand-in named [fallbackName] when it is gone */
    fun resolve(
        ref: MentionRef,
        fallbackName: String,
    ): MentionEntity = byRef[ref] ?: MentionEntity.DeletedMention(ref.id, fallbackName, ref.type)

    // names only one record carries, longest first - a shared name cannot be told apart in plain text
    internal val uniqueByName: List<MentionEntity> by lazy {
        entities
            .groupBy { it.name.foldPerChar() }
            .values
            .mapNotNull { it.singleOrNull() }
            .sortedByDescending { it.name.length }
    }

    companion object {
        val EMPTY = MentionIndex(emptyList())
    }
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

/** splits [text] on mention tokens, [autoMatch] also finds the bare whole-word names of records that are unique */
fun parseMentions(
    text: String,
    index: MentionIndex,
    autoMatch: Boolean = false,
): List<MentionSegment> {
    val autoNames = if (autoMatch) autoNamesOf(index) else emptyList()
    val segments = mutableListOf<MentionSegment>()
    var cursor = 0
    TOKEN_REGEX.findAll(text).forEach { token ->
        segments += plainSegments(text.substring(cursor, token.range.first), autoNames)
        segments += MentionSegment.Mention(index.resolve(token.toRef(), token.tokenName()))
        cursor = token.range.last + 1
    }
    segments += plainSegments(text.substring(cursor), autoNames)
    return segments
}

private fun plainSegments(
    text: String,
    autoNames: List<AutoName>,
): List<MentionSegment> =
    when {
        text.isEmpty() -> emptyList()
        autoNames.isEmpty() -> listOf(MentionSegment.Text(text))
        else -> splitOnAutoNames(text, autoNames)
    }

private fun splitOnAutoNames(
    text: String,
    autoNames: List<AutoName>,
): List<MentionSegment> {
    val segments = mutableListOf<MentionSegment>()
    val buffer = StringBuilder()
    val folded = text.foldPerChar()

    fun flush() {
        if (buffer.isNotEmpty()) {
            segments += MentionSegment.Text(buffer.toString())
            buffer.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        val auto = autoNames.matchAt(text, folded, i)
        if (auto != null) {
            flush()
            segments += MentionSegment.AutoMatch(text.substring(i, i + auto.folded.length), auto.entity)
            i += auto.folded.length
        } else {
            buffer.append(text[i])
            i++
        }
    }
    flush()
    return segments
}

// a space ends the mention token - also what closes the popover once insertMention's own trailing space lands
private val TRAILING_MENTION_REGEX = Regex("@([^@\\s\\[\\]|]{0,24})$")

/** the in-progress "@query" at the end of [draft], if the caret is mid-mention - drives the suggestion popover */
fun trailingMentionQuery(draft: String): String? = TRAILING_MENTION_REGEX.find(draft)?.groupValues?.get(1)

/** replaces the trailing "@query" in [draft] with [token] plus a trailing space */
fun insertMention(
    draft: String,
    token: String,
): String = TRAILING_MENTION_REGEX.replace(draft) { "$token " }

/** every distinct entity mentioned across [bodies], in first-seen order - the caller decides whether to cap it */
fun mentionsIn(
    bodies: List<String>,
    index: MentionIndex,
): List<MentionEntity> =
    bodies
        .flatMap { parseMentions(it, index) }
        .filterIsInstance<MentionSegment.Mention>()
        .map { it.entity }
        .distinctBy { it.dedupeKey() }

fun MentionEntity.dedupeKey(): String = "${ref.type.name.lowercase()}:${ref.id}"
