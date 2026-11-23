package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.core.analytics.AnalyticsEvent
import com.lyrismet.incadent.core.analytics.AnalyticsTracker
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportFailure
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportResult
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImporter
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedDuration
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedRange
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSchool
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Texts =
    SpellImportTexts(
        schools = mapOf(SpellSchool.EVOCATION to "Воплощение"),
        touch = "Касание",
        self = "На себя",
        feet = "фт",
    )

private fun imported(
    range: ImportedRange = ImportedRange.Feet(150),
    school: SpellSchool? = SpellSchool.EVOCATION,
    material: String? = null,
) = ImportedSpell(
    source = SpellSource.DND_SU,
    name = "Огненный шар",
    level = 3,
    school = school,
    castKind = CastKind.ACTION,
    castingTime = "",
    range = range,
    verbal = true,
    somatic = true,
    material = material,
    duration = ImportedDuration(DurationUnit.INSTANT, 0, ""),
    concentration = false,
    ritual = false,
    text = "8к6 урона",
    dice = listOf("8к6"),
)

private class FakeImporter(
    private val answer: suspend () -> SpellImportResult,
) : SpellImporter {
    var calls = 0

    override suspend fun import(link: String): SpellImportResult {
        calls++
        return answer()
    }
}

private class RecordingTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}

class SpellImportFlowTest {
    private val ui = SpellsUiState().apply { draft = CharacterSpell(id = 0, name = "", level = 1) }
    private val tracker = RecordingTracker()

    private fun flow(
        scope: TestScope,
        importer: SpellImporter,
    ) = SpellImportFlow(ui, scope, SpellImportDeps(importer, tracker)) { Texts }

    private fun typed(flow: SpellImportFlow): SpellImportFlow =
        flow.apply {
            open()
            changeLink("https://dnd.su/spells/1-x/")
        }

    @Test
    fun `a good import fills the form and keeps the hint and tells analytics`() =
        runTest {
            val flow = typed(flow(this, FakeImporter { SpellImportResult.Imported(imported()) }))

            flow.submit(position = 4)
            advanceUntilIdle()

            val draft = assertIs<CharacterSpell>(ui.draft)
            assertEquals("Огненный шар", draft.name)
            assertEquals(4, draft.position)
            assertEquals(0L, draft.id)
            assertNull(ui.importSheet)
            assertEquals(ImportHint("dnd.su", listOf("8к6")), ui.importHint)
            assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.SpellImported("dnd_su")), tracker.events)
        }

    @Test
    fun `a failed import keeps the sheet and the link and says why`() =
        runTest {
            val flow = typed(flow(this, FakeImporter { SpellImportResult.Failed(SpellImportFailure.OFFLINE) }))

            flow.submit(position = 0)
            advanceUntilIdle()

            val sheet = assertIs<SpellImportSheetState>(ui.importSheet)
            assertEquals("https://dnd.su/spells/1-x/", sheet.link)
            assertEquals(ImportPhase.Failed(SpellImportFailure.OFFLINE), sheet.phase)
            assertNull(ui.importHint)
            assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.SpellImportFailed("offline")), tracker.events)
        }

    @Test
    fun `editing the link clears the error`() =
        runTest {
            val flow = typed(flow(this, FakeImporter { SpellImportResult.Failed(SpellImportFailure.NOT_FOUND) }))
            flow.submit(position = 0)
            advanceUntilIdle()

            flow.changeLink("https://dnd.su/spells/2-y/")

            assertEquals(ImportPhase.Idle, ui.importSheet?.phase)
        }

    @Test
    fun `a second press while loading does not ask again`() =
        runTest {
            val gate = CompletableDeferred<SpellImportResult>()
            val importer = FakeImporter { gate.await() }
            val flow = typed(flow(this, importer))

            flow.submit(position = 0)
            runCurrent()
            flow.submit(position = 0)
            flow.changeLink("other")
            runCurrent()

            assertEquals(1, importer.calls)
            assertEquals(ImportPhase.Loading, ui.importSheet?.phase)
            gate.complete(SpellImportResult.Failed(SpellImportFailure.UNREADABLE))
        }

    @Test
    fun `closing the sheet while loading cancels the load`() =
        runTest {
            val gate = CompletableDeferred<SpellImportResult>()
            val flow = typed(flow(this, FakeImporter { gate.await() }))
            flow.submit(position = 0)
            runCurrent()

            flow.close()
            gate.complete(SpellImportResult.Imported(imported()))
            advanceUntilIdle()

            assertNull(ui.importSheet)
            assertEquals("", ui.draft?.name)
            assertNull(ui.importHint)
            assertTrue(tracker.events.isEmpty())
        }

    @Test
    fun `an empty link is not sent`() =
        runTest {
            val importer = FakeImporter { SpellImportResult.Imported(imported()) }
            val flow = flow(this, importer).apply { open() }

            flow.submit(position = 0)
            advanceUntilIdle()

            assertEquals(0, importer.calls)
        }

    @Test
    fun `the sheet opens only over the form of a new spell`() =
        runTest {
            val flow = flow(this, FakeImporter { SpellImportResult.Imported(imported()) })
            ui.draft = CharacterSpell(id = 7, name = "Щит", level = 1)
            flow.open()
            assertNull(ui.importSheet)

            ui.draft = CharacterSpell(id = 0, name = "", level = 1)
            ui.block = SpellBlock.DAMAGE
            flow.open()
            assertNull(ui.importSheet)

            ui.block = null
            flow.open()
            assertIs<SpellImportSheetState>(ui.importSheet)
        }

    @Test
    fun `ending the hint takes it away`() =
        runTest {
            val flow = typed(flow(this, FakeImporter { SpellImportResult.Imported(imported()) }))
            flow.submit(position = 0)
            advanceUntilIdle()

            flow.endHint()

            assertNull(ui.importHint)
        }

    @Test
    fun `a draft leaves damage growth and free casts empty`() {
        val draft = imported(material = "").toDraft(Texts, position = 2)

        assertTrue(draft.damage.isEmpty())
        assertEquals(0, draft.freeMax)
        assertEquals(1, draft.projectiles)
        assertEquals(DurationUnit.INSTANT, draft.durationUnit)
        assertTrue(draft.hasMaterial)
    }

    @Test
    fun `ranges and schools are written in the words of the app`() {
        assertEquals("150 фт", imported().toDraft(Texts, 0).range)
        assertEquals("Касание", imported(range = ImportedRange.Touch).toDraft(Texts, 0).range)
        assertEquals("На себя", imported(range = ImportedRange.Self).toDraft(Texts, 0).range)
        assertEquals("Зрение", imported(range = ImportedRange.Text("Зрение")).toDraft(Texts, 0).range)
        assertEquals("Воплощение", imported().toDraft(Texts, 0).school)
        assertEquals("", imported(school = null).toDraft(Texts, 0).school)
    }
}
