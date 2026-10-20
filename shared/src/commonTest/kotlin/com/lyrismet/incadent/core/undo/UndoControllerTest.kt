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

    @Test
    fun `a made bar carries its follow-up and the button closes it`() =
        runTest {
            val controller = UndoController(backgroundScope)
            var opened = false
            controller.showMade("t", "s", UndoFollowUp("Открыть") { opened = true }) {}
            assertEquals(
                "Открыть",
                controller.current.value
                    ?.followUp
                    ?.label,
            )
            assertTrue(controller.current.value?.isMade == true)

            controller.followUp()

            assertTrue(opened)
            assertNull(controller.current.value)
        }

    @Test
    fun `dropping the follow-up keeps the undo and the made look`() =
        runTest {
            val controller = UndoController(backgroundScope)
            var restored = false
            controller.showMade("t", "s", UndoFollowUp("Открыть") {}) { restored = true }

            controller.dropFollowUp()

            assertNull(controller.current.value?.followUp)
            assertTrue(controller.current.value?.isMade == true)
            controller.undo()
            runCurrent()
            assertTrue(restored)
        }

    @Test
    fun `a plain bar is not a made one and follow-up does nothing`() =
        runTest {
            val controller = UndoController(backgroundScope)
            controller.show("t", "s") {}
            assertTrue(controller.current.value?.isMade == false)

            controller.followUp()

            assertNull(controller.current.value)
        }
}
