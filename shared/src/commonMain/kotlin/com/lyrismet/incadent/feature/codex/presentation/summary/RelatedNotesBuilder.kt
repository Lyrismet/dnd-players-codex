package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.domain.parseMentions
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels

private const val SNIPPET_MAX_LENGTH = 110

private fun EntityRef.matches(entity: MentionEntity): Boolean =
    when (this) {
        is EntityRef.Npc -> entity is MentionEntity.NpcMention && entity.id == id
        is EntityRef.Location -> entity is MentionEntity.LocationMention && entity.id == id
        is EntityRef.Quest -> entity is MentionEntity.QuestMention && entity.id == id
        is EntityRef.Party -> entity is MentionEntity.PartyMention && entity.id == id
    }

/** every session with at least one note that `@mentions` [ref], newest first, with a snippet and hit count */
internal fun buildRelatedNotes(
    ref: EntityRef,
    lookup: EntityLookup,
): List<RelatedNoteItem> {
    val entriesBySession = lookup.sessionEntries.groupBy { it.sessionNoteId }
    val numberLabels = lookup.sessionNotes.sessionNumberLabels(lookup.sessionNumbering)
    return lookup.sessionNotes
        .sortedByDescending { it.sessionDate }
        .mapNotNull { note ->
            val hits =
                entriesBySession[note.id].orEmpty().filter { entry ->
                    parseMentions(entry.body, lookup.mentionIndex)
                        .filterIsInstance<MentionSegment.Mention>()
                        .any { ref.matches(it.entity) }
                }
            if (hits.isEmpty()) {
                null
            } else {
                RelatedNoteItem(
                    sessionNoteId = note.id,
                    entryId = hits.first().id,
                    numberLabel = numberLabels.getValue(note.id),
                    title = note.title,
                    dateLabel = note.sessionDate.toDisplayDate(),
                    snippet = plainTextSnippet(hits.first().body, lookup.mentionIndex),
                    matchCount = hits.size,
                )
            }
        }
}

private fun plainTextSnippet(
    body: String,
    candidates: MentionIndex,
): String {
    val plain =
        parseMentions(body, candidates).joinToString("") { segment ->
            when (segment) {
                is MentionSegment.Text -> segment.text
                is MentionSegment.Mention -> segment.entity.name
                is MentionSegment.AutoMatch -> segment.text
            }
        }
    return if (plain.length > SNIPPET_MAX_LENGTH) plain.take(SNIPPET_MAX_LENGTH - 1) + "…" else plain
}
