package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.domain.MentionBody
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.countMentionHits
import com.lyrismet.incadent.feature.mention.domain.dedupeKey
import com.lyrismet.incadent.feature.mention.domain.linkAutoMentions
import com.lyrismet.incadent.feature.mention.domain.relinkMentionIn
import com.lyrismet.incadent.feature.mention.domain.unlinkMentionIn
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.auto_mention_linked_title
import dndplayerscodex.shared.generated.resources.deleted_mention_linked_title
import dndplayerscodex.shared.generated.resources.deleted_mention_text_toast_title
import dndplayerscodex.shared.generated.resources.mention_created_fill
import dndplayerscodex.shared.generated.resources.mention_created_first_title
import dndplayerscodex.shared.generated.resources.mention_created_open
import dndplayerscodex.shared.generated.resources.mention_created_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the "?" tag sheet's content - absent (null on [SessionDetailState]) while it is closed */
data class DeletedMentionSheetState(
    val name: String,
    val type: EntityKind,
    val notes: Int,
    val sessions: Int,
    // a name typed after "@" that no record answers to yet - the sheet words itself for a record that never existed
    val isUnlinked: Boolean,
    // true while the "link to another record" list replaces the three actions
    val isPicking: Boolean,
    val query: String,
    // the live records a mention can be linked to that match [query], each told apart by colour and kind
    val linkOptions: List<DeletedMentionLinkOption>,
)

/** one record in the "link to another record" list - [id] is the record's dedupe key, stable while the list shifts */
data class DeletedMentionLinkOption(
    val id: String,
    val name: String,
    val glyph: MentionGlyph,
    val color: StatusColor,
    val type: EntityKind,
)

/** one record a deleted mention can be linked to - the select option and what picking it writes into the notes */
internal class MentionLinkTarget(
    val id: String,
    val entity: MentionEntity,
)

/** the texts an undo toast of a mention rewrite shows, resolved once by the presenter */
internal class MentionTitles(
    val autoLinked: String,
    val deletedLinked: String,
    val deletedToText: String,
    val created: CreatedTitles,
)

/** the texts of the bar shown after a record is made from a typed name, the first one worded differently */
internal class CreatedTitles(
    val title: String,
    val firstTitle: String,
    val fill: String,
    val open: String,
)

/** everything the mention events read - one value instead of one parameter per piece */
internal class MentionEventData(
    val candidates: MentionIndex,
    val allEntries: List<SessionEntry>,
    val linkTargets: List<MentionLinkTarget>,
    val titles: MentionTitles,
)

/** how many notes in how many sessions still carry the tag of [deleted] - across every session, not just this one */
internal fun MentionEventData.hitsOf(deleted: DeletedMention) =
    countMentionHits(
        MentionEntity.DeletedMention(deleted.id, deleted.name, deleted.type),
        allEntries.map { MentionBody(it.sessionNoteId, it.body) },
        candidates,
    )

/** writes the mention changes that touch note texts, each one undoable and never silent */
internal class SessionMentionActions(
    private val entryRepository: SessionEntryRepository,
    private val undoController: UndoController,
) {
    /** puts "@" on the auto matched words of [entry] - only those of [only] when given - and settles its check */
    fun linkAutoMatches(
        scope: CoroutineScope,
        entry: SessionEntry,
        data: MentionEventData,
        only: MentionEntity?,
    ) {
        val linked = linkAutoMentions(entry.body, data.candidates, only)
        scope.launch {
            if (linked != entry.body) entryRepository.rewriteBody(entry.id, linked)
            // a single word leaves the others still waiting for a decision
            if (only == null) entryRepository.setMentionCheck(entry.id, false)
            if (linked != entry.body) {
                undoController.show(data.titles.autoLinked, linked.take(UNDO_PREVIEW_LENGTH)) {
                    entryRepository.rewriteBody(entry.id, entry.body)
                    entryRepository.setMentionCheck(entry.id, entry.mentionCheck)
                }
            }
        }
    }

    fun dismissAutoMatches(
        scope: CoroutineScope,
        entryId: Long,
    ) {
        scope.launch { entryRepository.setMentionCheck(entryId, false) }
    }

    /** turns the "?" tag of [deleted] into a link to [target], or into plain words when [target] is null */
    fun rewriteDeleted(
        scope: CoroutineScope,
        deleted: DeletedMention,
        target: MentionEntity?,
        data: MentionEventData,
    ) {
        val ref = MentionRef(deleted.type, deleted.id)
        val changes =
            rewrittenEntries(data.allEntries) { body ->
                if (target == null) unlinkMentionIn(body, ref) else relinkMentionIn(body, ref, target)
            }
        val title = if (target == null) data.titles.deletedToText else data.titles.deletedLinked
        scope.launch {
            val revert = writeBodies(changes)
            undoController.show(title, deleted.name) { revert() }
        }
    }

    /** points every token of [from] at the record [to] named [toName], and returns the way back */
    suspend fun relinkAll(
        from: MentionRef,
        to: MentionRef,
        toName: String,
        entries: List<SessionEntry>,
    ): suspend () -> Unit = writeBodies(rewrittenEntries(entries) { relinkMentionIn(it, from, to, toName) })

    // only the entries a rewrite actually changes, each with its new body
    private fun rewrittenEntries(
        entries: List<SessionEntry>,
        rewrite: (String) -> String,
    ): List<Pair<SessionEntry, String>> =
        entries.mapNotNull { entry ->
            val rewritten = rewrite(entry.body)
            (entry to rewritten).takeIf { rewritten != entry.body }
        }

    // writes the new bodies and returns the way back to the old ones
    private suspend fun writeBodies(changes: List<Pair<SessionEntry, String>>): suspend () -> Unit {
        changes.forEach { (entry, body) -> entryRepository.rewriteBody(entry.id, body) }
        return { changes.forEach { (entry, _) -> entryRepository.rewriteBody(entry.id, entry.body) } }
    }

    private companion object {
        const val UNDO_PREVIEW_LENGTH = 60
    }
}

/** the live records a "?" tag can be linked to, each told apart by its dedupe key */
internal fun mentionLinkTargets(entities: List<MentionEntity>): List<MentionLinkTarget> =
    entities.map { MentionLinkTarget(it.dedupeKey(), it) }

/** the data the mention events read, with the texts of their undo toasts resolved from resources */
@Composable
internal fun mentionEventData(
    candidates: MentionIndex,
    entities: List<MentionEntity>,
    allEntries: List<SessionEntry>,
): MentionEventData =
    MentionEventData(
        candidates = candidates,
        allEntries = allEntries,
        linkTargets = mentionLinkTargets(entities),
        titles =
            MentionTitles(
                autoLinked = stringResource(Res.string.auto_mention_linked_title),
                deletedLinked = stringResource(Res.string.deleted_mention_linked_title),
                deletedToText = stringResource(Res.string.deleted_mention_text_toast_title),
                created =
                    CreatedTitles(
                        title = stringResource(Res.string.mention_created_title),
                        firstTitle = stringResource(Res.string.mention_created_first_title),
                        fill = stringResource(Res.string.mention_created_fill),
                        open = stringResource(Res.string.mention_created_open),
                    ),
            ),
    )

/** the records that match [query] by name, as the list rows - everything when nothing is typed */
internal fun MentionEventData.linkOptionsFor(query: String): List<DeletedMentionLinkOption> {
    val needle = query.trim().foldForSearch()
    return linkTargets
        .filter {
            needle.isEmpty() ||
                it.entity.name
                    .foldForSearch()
                    .contains(needle)
        }.map { target ->
            val chip = target.entity.toChipItem()
            DeletedMentionLinkOption(target.id, chip.label, chip.glyph, chip.color, target.entity.ref.type)
        }
}
