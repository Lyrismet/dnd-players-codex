package com.lyrismet.incadent.core.undo

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val WINDOW_MS = 5000L

class UndoControllerTest {
    @Test
    fun `shown action disappears once the undo window closes`() =
        runTest {
            val controller = UndoController(backgroundScope)

            controller.show(title = "Сессия удалена", subtitle = "Отменить") {}
            advanceTimeBy(WINDOW_MS - 1)
            assertEquals("Сессия удалена", controller.current.value?.title)

            advanceTimeBy(1)
            runCurrent()
            assertNull(controller.current.value)
        }

    @Test
    fun `undo runs the restore and clears the toast`() =
        runTest {
            val controller = UndoController(backgroundScope)
            var restored = false
            controller.show(title = "t", subtitle = "s") { restored = true }

            controller.undo()
            runCurrent()

            assertTrue(restored)
            assertNull(controller.current.value)
        }

    @Test
    fun `a newer action is not cleared by the timer of the one it replaced`() =
        runTest {
            val controller = UndoController(backgroundScope)
            controller.show(title = "first", subtitle = "s") {}
            advanceTimeBy(4000)

            controller.show(title = "second", subtitle = "s") {}
            advanceTimeBy(4000)

            assertEquals("second", controller.current.value?.title)
        }

    @Test
    fun `a failing restore still clears the toast without throwing`() =
        runTest {
            val controller = UndoController(backgroundScope)
            controller.show(title = "t", subtitle = "s") { error("restore failed") }

            controller.undo()
            runCurrent()

            assertNull(controller.current.value)
        }
}
