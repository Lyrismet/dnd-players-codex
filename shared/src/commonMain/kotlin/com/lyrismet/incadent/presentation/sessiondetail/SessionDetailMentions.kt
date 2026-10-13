package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.designsystem.component.toChipItem
import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.core.mention.MentionBody
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.countMentionHits
import com.lyrismet.incadent.core.mention.dedupeKey
import com.lyrismet.incadent.core.mention.linkAutoMentions
import com.lyrismet.incadent.core.mention.mentionKey
import com.lyrismet.incadent.core.mention.renameMentionIn
import com.lyrismet.incadent.core.mention.unlinkMentionIn
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.repository.DeletedMentionRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.presentation.codex.CodexEntryType
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.auto_mention_linked_title
import dndplayerscodex.shared.generated.resources.deleted_mention_linked_title
import dndplayerscodex.shared.generated.resources.deleted_mention_text_toast_title
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the "?" tag sheet's content - absent (null on [SessionDetailState]) while it is closed */
data class DeletedMentionSheetState(
    val name: String,
    val type: DeletedMentionType,
    val notes: Int,
    val sessions: Int,
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
    val type: DeletedMentionType,
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
)

/** everything the mention events read - one value instead of one parameter per piece */
internal class MentionEventData(
    val candidates: List<MentionCandidate>,
    val questPrefix: String,
    val allEntries: List<SessionEntry>,
    val deleted: List<DeletedMention>,
    val linkTargets: List<MentionLinkTarget>,
    val titles: MentionTitles,
)

internal fun MentionEventData.deletedMatching(snapshot: DeletedMention): DeletedMention? =
    deleted.firstOrNull { it == snapshot }

/** how many notes in how many sessions still carry the tag of [deleted] - across every session, not just this one */
internal fun MentionEventData.hitsOf(deleted: DeletedMention) =
    countMentionHits(
        MentionEntity.DeletedMention(deleted.name, deleted.type),
        allEntries.map { MentionBody(it.sessionNoteId, it.body) },
        candidates,
    )

/** writes the mention changes that touch note texts, each one undoable and never silent */
internal class SessionMentionActions(
    private val entryRepository: SessionEntryRepository,
    private val deletedRepository: DeletedMentionRepository,
    private val undoController: UndoController,
) {
    /** puts "@" on the auto matched words of [entry] - only those of [only] when given - and settles its check */
    fun linkAutoMatches(
        scope: CoroutineScope,
        entry: SessionEntry,
        data: MentionEventData,
        only: MentionEntity?,
    ) {
        val linked = linkAutoMentions(entry.body, data.candidates, data.questPrefix, only)
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
        val oldKey = mentionKey(MentionEntity.DeletedMention(deleted.name, deleted.type), data.questPrefix)
        val otherKeys = data.candidates.map { it.key }
        val changes =
            data.allEntries.mapNotNull { entry ->
                val rewritten =
                    if (target == null) {
                        unlinkMentionIn(entry.body, oldKey, deleted.name, otherKeys)
                    } else {
                        renameMentionIn(entry.body, oldKey, mentionKey(target, data.questPrefix), otherKeys)
                    }
                (entry to rewritten).takeIf { rewritten != entry.body }
            }
        val title = if (target == null) data.titles.deletedToText else data.titles.deletedLinked
        scope.launch {
            changes.forEach { (entry, body) -> entryRepository.rewriteBody(entry.id, body) }
            deletedRepository.forget(deleted)
            undoController.show(title, deleted.name) {
                changes.forEach { (entry, _) -> entryRepository.rewriteBody(entry.id, entry.body) }
                deletedRepository.remember(deleted)
            }
        }
    }

    private companion object {
        const val UNDO_PREVIEW_LENGTH = 60
    }
}

/** the live records a "?" tag can be linked to, each told apart by its dedupe key */
internal fun mentionLinkTargets(entities: List<MentionEntity>): List<MentionLinkTarget> =
    entities.map { MentionLinkTarget(it.dedupeKey(), it) }

internal fun DeletedMentionType.toEntryType(): CodexEntryType =
    when (this) {
        DeletedMentionType.NPC -> CodexEntryType.NPC
        DeletedMentionType.PARTY -> CodexEntryType.PARTY
        DeletedMentionType.QUEST -> CodexEntryType.QUEST
        DeletedMentionType.LOCATION -> CodexEntryType.LOCATION
    }

/** the data the mention events read, with the texts of their undo toasts resolved from resources */
@Composable
internal fun mentionEventData(
    candidates: List<MentionCandidate>,
    entities: List<MentionEntity>,
    allEntries: List<SessionEntry>,
    deleted: List<DeletedMention>,
): MentionEventData =
    MentionEventData(
        candidates = candidates,
        questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix),
        allEntries = allEntries,
        deleted = deleted,
        linkTargets = mentionLinkTargets(entities),
        titles =
            MentionTitles(
                autoLinked = stringResource(Res.string.auto_mention_linked_title),
                deletedLinked = stringResource(Res.string.deleted_mention_linked_title),
                deletedToText = stringResource(Res.string.deleted_mention_text_toast_title),
            ),
    )

/** a record created again takes its mentions back, so the snapshot that stood in for it is dropped */
@Composable
internal fun ForgetRevivedMentions(
    revived: List<DeletedMention>,
    repository: DeletedMentionRepository,
) {
    LaunchedEffect(revived) { revived.forEach { repository.forget(it) } }
}

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
            DeletedMentionLinkOption(target.id, chip.label, chip.glyph, chip.color, target.entity.mentionType())
        }
}

private fun MentionEntity.mentionType(): DeletedMentionType =
    when (this) {
        is MentionEntity.NpcMention -> DeletedMentionType.NPC
        is MentionEntity.PartyMention -> DeletedMentionType.PARTY
        is MentionEntity.LocationMention -> DeletedMentionType.LOCATION
        is MentionEntity.QuestMention -> DeletedMentionType.QUEST
        is MentionEntity.DeletedMention -> type
    }
