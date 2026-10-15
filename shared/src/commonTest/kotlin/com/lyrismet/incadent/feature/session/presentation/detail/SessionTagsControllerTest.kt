package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTagsControllerTest {
    private val repository = FakeTagRepository()
    private val controller = SessionTagsController(7, repository, mutableStateOf(false), mutableStateOf(""))

    @Test
    fun `the sheet opens and closes`() {
        controller.open()
        assertTrue(controller.isOpen.value)

        controller.dismiss()
        assertFalse(controller.isOpen.value)
    }

    @Test
    fun `toggling assigns a tag and toggling it again removes it`() =
        runTest {
            controller.toggle(this, "Бой", assigned = emptyList())
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Бой"), repository.tagsOf(7))

            controller.toggle(this, "бой", assigned = listOf("Бой"))
            testScheduler.advanceUntilIdle()
            assertEquals(emptyList(), repository.tagsOf(7))
        }

    @Test
    fun `adding every suggestion assigns them all`() =
        runTest {
            controller.addAll(this, listOf("Сюжет", "Отдых"))
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("Сюжет", "Отдых"), repository.tagsOf(7))
        }

    @Test
    fun `a typed custom name is normalized assigned and the field cleared`() =
        runTest {
            controller.onDraftChanged("  засада у моста  ")
            controller.submitDraft(this)
            testScheduler.advanceUntilIdle()

            assertEquals("", controller.draft.value)
            assertEquals(1, repository.tagsOf(7).size)
            assertTrue(repository.tagsOf(7).single().startsWith("Засада"))
        }

    @Test
    fun `a blank draft assigns nothing and keeps the field`() =
        runTest {
            controller.onDraftChanged("   ")
            controller.submitDraft(this)
            testScheduler.advanceUntilIdle()

            assertEquals(emptyList(), repository.tagsOf(7))
            assertEquals("   ", controller.draft.value)
        }
}
