package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.feature.campaign.domain.exchange.NoteOverlap
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.PlannedRecord
import com.lyrismet.incadent.feature.campaign.domain.model.PlannedSession
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.RecordStatus
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
import com.lyrismet.incadent.feature.campaign.domain.model.SessionStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportCompareTest {
    private fun ref(id: Long) = ExchangeRef(ExchangeKind.NPC, id)

    private fun record(
        id: Long,
        status: RecordStatus,
        changes: List<FieldChange> = emptyList(),
    ) = PlannedRecord(ref(id), "R$id", status, changes = changes)

    private val conflict = FieldChange(RecordField.DESCRIPTION, "mine", "theirs")
    private val fill = FieldChange(RecordField.RACE, "", "elf")

    private val plan =
        ImportPlan(
            mode = ExportMode.FRIEND,
            records =
                listOf(
                    record(1, RecordStatus.NEW),
                    record(2, RecordStatus.CHANGED, listOf(conflict, fill)),
                    record(3, RecordStatus.SIMILAR),
                    record(4, RecordStatus.SAME_NAME, listOf(fill)),
                    record(5, RecordStatus.SAME),
                    record(6, RecordStatus.DELETED_BEFORE),
                ),
            sessions =
                listOf(
                    PlannedSession(1, "Огни", LocalDate(2026, 10, 24), SessionStatus.MERGE, NoteOverlap(6, 4, 2)),
                    PlannedSession(2, "Мост", LocalDate(2026, 9, 2), SessionStatus.NEW, NoteOverlap(0, 1, 0)),
                ),
            personal = null,
        )

    @Test
    fun `only a similar record and a real conflict need an answer`() {
        val view = buildCompare(plan, null, CompareAnswers())

        assertEquals(2, view.needTotal)
        assertEquals(1, view.similar.size)
        assertEquals(1, view.conflicts.size)
        assertEquals(1, view.deleted.size)
    }

    @Test
    fun `nothing is decided before an answer and a default choice does not count`() {
        assertEquals(0, buildCompare(plan, null, CompareAnswers()).needDone)
    }

    @Test
    fun `answering a similar record and touching a conflict count as decided`() {
        val answers =
            CompareAnswers(
                similar = mapOf(ref(3) to Resolution.APART),
                fields = mapOf(ref(2) to mapOf(RecordField.DESCRIPTION to FieldChoice.FILE)),
            )

        val view = buildCompare(plan, null, answers)

        assertEquals(2, view.needDone)
        assertEquals(CompareBadge.FILE, view.conflicts.single().badge)
    }

    @Test
    fun `a record left for later is undecided`() {
        val answers = CompareAnswers(similar = mapOf(ref(3) to Resolution.LATER))

        assertEquals(1, buildCompare(plan, null, answers).undecided)
    }

    @Test
    fun `fields the file fills in count unless the person leaves them out`() {
        assertEquals(2, buildCompare(plan, null, CompareAnswers()).fillFields)

        val left = CompareAnswers(fields = mapOf(ref(2) to mapOf(RecordField.RACE to FieldChoice.MINE)))
        assertEquals(1, buildCompare(plan, null, left).fillFields)
    }

    @Test
    fun `names that match are one record only while the person agrees`() {
        val agreed = buildCompare(plan, null, CompareAnswers())
        val apart = buildCompare(plan, null, CompareAnswers(mergeNameTwins = false))

        assertEquals(1, agreed.nameTwins)
        assertEquals(1, agreed.fillRecords - 1)
        assertEquals(agreed.newCount + 1, apart.newCount)
        assertEquals(1, apart.fillRecords)
    }

    @Test
    fun `an open record shows its conflicts and the fields it fills`() {
        val view = buildCompare(plan, null, CompareAnswers(open = ref(2)))

        val detail = view.detail
        assertEquals(1, detail?.fields?.size)
        assertEquals(1, detail?.fills?.size)
        assertEquals(FieldChoice.MINE, detail?.fields?.single()?.choice)
        assertTrue(detail?.fills?.single()?.taken == true)
    }

    @Test
    fun `a session with notes of its own is merged unless kept apart`() {
        assertTrue(buildCompare(plan, null, CompareAnswers()).sessions.single().merge)
        assertFalse(buildCompare(plan, null, CompareAnswers(apartSessions = setOf(1L))).sessions.single().merge)
    }

    @Test
    fun `a file that holds only what the campaign has is nothing new`() {
        val same =
            plan.copy(
                records = listOf(record(1, RecordStatus.SAME), record(2, RecordStatus.SAME_NAME)),
                sessions = emptyList(),
            )

        assertTrue(buildCompare(same, null, CompareAnswers()).nothingNew)
        assertFalse(buildCompare(plan, null, CompareAnswers()).nothingNew)
    }

    @Test
    fun `the answers change with the events`() {
        val after =
            CompareAnswers()
                .reduce(ImportEvent.AllFieldsChosen(FieldChoice.FILE), plan)
                .reduce(ImportEvent.FillToggled(ref(2), RecordField.RACE), plan)

        assertEquals(FieldChoice.FILE, after.fields.getValue(ref(2)).getValue(RecordField.DESCRIPTION))
        assertEquals(FieldChoice.MINE, after.fields.getValue(ref(2)).getValue(RecordField.RACE))
        assertNull(CompareAnswers().reduce(ImportEvent.RecordOpened(null), plan).open)
    }

    @Test
    fun `a second tap on an answer takes it back`() {
        val once = CompareAnswers().reduce(ImportEvent.SimilarAnswered(ref(3), Resolution.SAME), plan)
        val twice = once.reduce(ImportEvent.SimilarAnswered(ref(3), Resolution.SAME), plan)

        assertEquals(Resolution.SAME, once.similar[ref(3)])
        assertNull(twice.similar[ref(3)])
    }
}
