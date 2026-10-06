package com.lyrismet.incadent.core.entitysummary

import com.lyrismet.incadent.core.format.sessionNumberLabels
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.MentionSegment
import com.lyrismet.incadent.core.mention.parseMentions

private const val SNIPPET_MAX_LENGTH = 110

// party members are never @mentioned in notes, so a party ref can't match any parsed mention
private fun EntityRef.matches(entity: MentionEntity): Boolean =
    when (this) {
        is EntityRef.Npc -> entity is MentionEntity.NpcMention && entity.id == id
        is EntityRef.Location -> entity is MentionEntity.LocationMention && entity.id == id
        is EntityRef.Quest -> entity is MentionEntity.QuestMention && entity.id == id
        is EntityRef.Party -> false
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
                    parseMentions(entry.body, lookup.mentionCandidates)
                        .filterIsInstance<MentionSegment.Mention>()
                        .any { ref.matches(it.entity) }
                }
            if (hits.isEmpty()) {
                null
            } else {
                RelatedNoteItem(
                    sessionNoteId = note.id,
                    numberLabel = numberLabels.getValue(note.id),
                    title = note.title,
                    dateLabel = note.sessionDate.toDisplayDate(),
                    snippet = plainTextSnippet(hits.first().body, lookup.mentionCandidates),
                    matchCount = hits.size,
                )
            }
        }
}

private fun plainTextSnippet(
    body: String,
    candidates: List<MentionCandidate>,
): String {
    val plain =
        parseMentions(body, candidates).joinToString("") { segment ->
            when (segment) {
                is MentionSegment.Text -> segment.text
                is MentionSegment.Mention -> segment.entity.name
            }
        }
    return if (plain.length > SNIPPET_MAX_LENGTH) plain.take(SNIPPET_MAX_LENGTH - 1) + "…" else plain
}
