package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.feature.campaign.domain.exchange.NoteOverlap
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.PlannedRecord
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordStatus
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
import com.lyrismet.incadent.feature.campaign.domain.model.SessionStatus
import kotlinx.datetime.LocalDate

// the search over the conflicts shows up only when there are enough of them to scroll through
private const val SEARCH_FROM = 10

/** a record of the file that only looks like one in the campaign: the person says if it is the same */
data class CompareSimilar(
    val ref: ExchangeRef,
    val mineName: String,
    val fileName: String,
    val choice: Resolution?,
)

/** who wins in a conflict row: what the campaign has, what the file has, or a mix picked field by field */
enum class CompareBadge { MINE, FILE, MIXED }

/** a record on both sides that differs in a field both have filled in */
data class CompareConflict(
    val ref: ExchangeRef,
    val name: String,
    val fields: List<RecordField>,
    val badge: CompareBadge,
)

/** one conflicting field of an open record: [choice] says which text stays */
data class CompareField(
    val change: FieldChange,
    val choice: FieldChoice,
)

/** a field the campaign has empty and the file fills, [taken] says whether it is taken */
data class CompareFill(
    val change: FieldChange,
    val taken: Boolean,
)

/** the screen of one record the person opened from the list of conflicts */
data class CompareDetail(
    val ref: ExchangeRef,
    val name: String,
    val fields: List<CompareField>,
    val fills: List<CompareFill>,
)

/** a session of the file that shares its day and title with one already there and brings notes of its own */
data class CompareSession(
    val fileId: Long,
    val title: String,
    val day: LocalDate,
    val overlap: NoteOverlap,
    val merge: Boolean,
)

data class CompareDeleted(
    val ref: ExchangeRef,
    val name: String,
    val brought: Boolean,
)

/** how a personal copy is offered: [ownCopy] is the person's own backup, else someone else's file with two switches */
data class ComparePersonal(
    val ownCopy: Boolean,
    val characterName: String?,
    val takeAsMine: Boolean,
    val takeVitals: Boolean,
)

/** everything the comparison step shows, built from the plan and the answers given so far */
data class CompareView(
    val newCount: Int,
    val changedCount: Int,
    val sessionCount: Int,
    val similar: List<CompareSimilar>,
    val conflicts: List<CompareConflict>,
    val hasSearch: Boolean,
    val query: String,
    val needTotal: Int,
    val needDone: Int,
    val undecided: Int,
    val nameTwins: Int,
    val mergeNameTwins: Boolean,
    val fillFields: Int,
    val fillRecords: Int,
    val newByKind: Map<ExchangeKind, Int>,
    val sessions: List<CompareSession>,
    val newSessions: List<LocalDate>,
    val deleted: List<CompareDeleted>,
    val personal: ComparePersonal?,
    val detail: CompareDetail?,
    val nothingNew: Boolean,
    val emptyRecords: Int,
    val emptySessions: Int,
) {
    val hasNeed: Boolean get() = needTotal > 0
}

/** what a person has answered so far, the plan itself never changes */
data class CompareAnswers(
    val similar: Map<ExchangeRef, Resolution> = emptyMap(),
    val fields: Map<ExchangeRef, Map<RecordField, FieldChoice>> = emptyMap(),
    val apartSessions: Set<Long> = emptySet(),
    val restore: Set<ExchangeRef> = emptySet(),
    val mergeNameTwins: Boolean = true,
    val open: ExchangeRef? = null,
    val query: String = "",
    val takeAsMine: Boolean = false,
    val takeVitals: Boolean = false,
)

internal fun ExchangeRef.isIn(scope: RecordScope?): Boolean =
    scope == null ||
        when (kind) {
            ExchangeKind.SESSION -> id in scope.sessions
            ExchangeKind.PLACE -> id in scope.places
            ExchangeKind.NPC -> id in scope.npcs
            ExchangeKind.QUEST -> id in scope.quests
            ExchangeKind.SPECIES -> id in scope.species
            ExchangeKind.PARTY -> id in scope.party
        }

private fun CompareAnswers.choiceOf(
    ref: ExchangeRef,
    change: FieldChange,
): FieldChoice = fields[ref]?.get(change.field) ?: if (change.isFill) FieldChoice.FILE else FieldChoice.MINE

private fun PlannedRecord.conflicts(): List<FieldChange> = changes.filter { !it.isFill }

private fun PlannedRecord.fills(): List<FieldChange> = changes.filter { it.isFill }

/** the comparison of [plan] limited to what [scope] takes, with the answers [answers] applied */
@Suppress("LongMethod")
fun buildCompare(
    plan: ImportPlan,
    scope: RecordScope?,
    answers: CompareAnswers,
): CompareView {
    val records = plan.records.filter { it.ref.isIn(scope) }
    val sessions = plan.sessions.filter { ExchangeRef(ExchangeKind.SESSION, it.fileId).isIn(scope) }
    val twins = records.filter { it.status == RecordStatus.SAME_NAME }
    // a record the file shares by name only counts as one record when the person lets them be
    val both =
        records.filter {
            it.status == RecordStatus.CHANGED ||
                (answers.mergeNameTwins && it.status == RecordStatus.SAME_NAME)
        }
    val conflicting = both.filter { it.conflicts().isNotEmpty() }
    val filling = both.filter { it.fills().isNotEmpty() }
    val similar = records.filter { it.status == RecordStatus.SIMILAR }
    val query = answers.query.trim().lowercase()
    val deleted = records.filter { it.status == RecordStatus.DELETED_BEFORE }
    val fresh = records.filter { it.status == RecordStatus.NEW }
    val merging = sessions.filter { it.status == SessionStatus.MERGE }
    val newSessions = sessions.filter { it.status == SessionStatus.NEW }
    val restored = deleted.count { it.ref in answers.restore }
    return CompareView(
        newCount =
            fresh.size + restored + similar.count { answers.similar[it.ref] == Resolution.APART } +
                if (answers.mergeNameTwins) 0 else twins.size,
        changedCount = conflicting.size,
        sessionCount = merging.size + newSessions.size,
        similar = similar.map { CompareSimilar(it.ref, it.similarTo.orEmpty(), it.name, answers.similar[it.ref]) },
        conflicts =
            conflicting
                .filter {
                    query.isEmpty() ||
                        it.name.lowercase().contains(
                            query,
                        )
                }.map { it.toConflict(answers) },
        hasSearch = conflicting.size >= SEARCH_FROM,
        query = answers.query,
        needTotal = similar.size + conflicting.size,
        needDone =
            similar.count { answers.similar[it.ref] != null } +
                conflicting.count { answers.fields[it.ref]?.isNotEmpty() == true },
        undecided = similar.count { (answers.similar[it.ref] ?: Resolution.LATER) == Resolution.LATER },
        nameTwins = twins.size,
        mergeNameTwins = answers.mergeNameTwins,
        fillFields = filling.sumOf { r -> r.fills().count { answers.choiceOf(r.ref, it) != FieldChoice.MINE } },
        fillRecords = filling.size,
        newByKind = fresh.groupingBy { it.ref.kind }.eachCount(),
        sessions =
            merging.map {
                CompareSession(
                    it.fileId,
                    it.title,
                    it.day,
                    it.overlap,
                    it.fileId !in answers.apartSessions,
                )
            },
        newSessions = newSessions.map { it.day }.sorted(),
        deleted = deleted.map { CompareDeleted(it.ref, it.name, it.ref in answers.restore) },
        personal =
            plan.personal?.let {
                ComparePersonal(
                    it.ownCopy,
                    it.characterName,
                    answers.takeAsMine,
                    answers.takeVitals,
                )
            },
        detail = both.firstOrNull { it.ref == answers.open }?.toDetail(answers),
        nothingNew =
            both.all { it.changes.isEmpty() } &&
                similar.isEmpty() &&
                fresh.isEmpty() &&
                deleted.isEmpty() &&
                newSessions.isEmpty() &&
                merging.isEmpty() &&
                (answers.mergeNameTwins || twins.isEmpty()),
        emptyRecords = records.size,
        emptySessions = sessions.size,
    )
}

private fun PlannedRecord.toConflict(answers: CompareAnswers): CompareConflict {
    val choices = conflicts().map { answers.choiceOf(ref, it) }
    val badge =
        when {
            choices.all { it == FieldChoice.MINE } -> CompareBadge.MINE
            choices.all { it == FieldChoice.FILE } -> CompareBadge.FILE
            else -> CompareBadge.MIXED
        }
    return CompareConflict(ref, name, conflicts().map { it.field }, badge)
}

private fun PlannedRecord.toDetail(answers: CompareAnswers): CompareDetail =
    CompareDetail(
        ref,
        name,
        conflicts().map { CompareField(it, answers.choiceOf(ref, it)) },
        fills().map { CompareFill(it, answers.choiceOf(ref, it) != FieldChoice.MINE) },
    )

private fun Map<ExchangeRef, Map<RecordField, FieldChoice>>.withChoice(
    ref: ExchangeRef,
    field: RecordField,
    choice: FieldChoice,
): Map<ExchangeRef, Map<RecordField, FieldChoice>> = this + (ref to (this[ref].orEmpty() + (field to choice)))

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

/** the answers after [event], the fields of every record both sides have come from [plan] */
@Suppress("CyclomaticComplexMethod")
fun CompareAnswers.reduce(
    event: CompareEvent,
    plan: ImportPlan?,
): CompareAnswers =
    when (event) {
        is ImportEvent.SimilarAnswered ->
            copy(
                similar =
                    if (similar[event.ref] ==
                        event.resolution
                    ) {
                        similar - event.ref
                    } else {
                        similar + (event.ref to event.resolution)
                    },
            )
        is ImportEvent.FieldChosen -> copy(fields = fields.withChoice(event.ref, event.field, event.choice))
        is ImportEvent.FillToggled -> {
            val taken = fields[event.ref]?.get(event.field) != FieldChoice.MINE
            copy(fields = fields.withChoice(event.ref, event.field, if (taken) FieldChoice.MINE else FieldChoice.FILE))
        }
        is ImportEvent.AllFieldsChosen ->
            copy(
                fields =
                    plan
                        ?.records
                        .orEmpty()
                        .filter { it.status == RecordStatus.CHANGED || it.status == RecordStatus.SAME_NAME }
                        .fold(fields) { acc, record ->
                            record.conflicts().fold(
                                acc,
                            ) { inner, change -> inner.withChoice(record.ref, change.field, event.choice) }
                        },
            )
        is ImportEvent.RecordOpened -> copy(open = event.ref)
        ImportEvent.NameTwinsToggled -> copy(mergeNameTwins = !mergeNameTwins)
        is ImportEvent.QueryChanged -> copy(query = event.text)
        is ImportEvent.SessionMerged ->
            copy(apartSessions = if (event.merge) apartSessions - event.fileId else apartSessions + event.fileId)
        is ImportEvent.BroughtBack -> copy(restore = restore.toggled(event.ref))
        ImportEvent.TakeAsMineToggled -> copy(takeAsMine = !takeAsMine)
        ImportEvent.TakeVitalsToggled -> copy(takeVitals = !takeVitals)
        is ImportEvent.OwnCopyChosen -> copy(takeAsMine = event.fromFile)
    }
