package com.lyrismet.incadent.core.selectlist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectListTest {
    private val options =
        listOf(
            SelectOption(1, "Мирдейл", searchText = "Мирдейл Долина"),
            SelectOption(
                2,
                "Храм Рассвета",
                subtitle = "Долина › Мирдейл",
                searchText = "Храм Рассвета Долина › Мирдейл",
            ),
            SelectOption(3, "Ёлки"),
            SelectOption(4, "Башня"),
        )

    private fun ids(rows: List<SelectRow>) = rows.map { it.option.id }

    @Test
    fun `without a query every option is shown ordered by title with yo sorted as ye`() {
        assertEquals(listOf(4L, 3L, 1L, 2L), ids(selectRows(options, "", emptySet())))
    }

    @Test
    fun `a query matches the search text so a path hit finds the nested option`() {
        val rows = selectRows(options, "мирдейл", emptySet())

        assertEquals(listOf(1L, 2L), ids(rows))
    }

    @Test
    fun `the match range marks the hit in the title and is null for a hit in the path only`() {
        val rows = selectRows(options, "мирд", emptySet())

        assertEquals(0..3, rows.first { it.option.id == 1L }.match)
        assertNull(rows.first { it.option.id == 2L }.match)
    }

    @Test
    fun `recent options come first in their own order`() {
        val rows = selectRows(options, "", emptySet(), recentIds = listOf(2, 4))

        assertEquals(listOf(2L, 4L, 3L, 1L), ids(rows))
    }

    @Test
    fun `checked options float to the top only without a query and only when asked to`() {
        val checked = setOf(1L)

        assertEquals(1L, ids(selectRows(options, "", checked, floatSelected = true)).first())
        assertEquals(4L, ids(selectRows(options, "", checked, floatSelected = false)).first())
        assertEquals(listOf(1L, 2L), ids(selectRows(options, "мирдейл", checked, floatSelected = true)))
    }

    @Test
    fun `excluded options never show up`() {
        val rows = selectRows(options, "", emptySet(), excludedIds = setOf(1, 2))

        assertEquals(listOf(4L, 3L), ids(rows))
    }

    @Test
    fun `rows know whether they are selected`() {
        val rows = selectRows(options, "", setOf(3L))

        assertTrue(rows.first { it.option.id == 3L }.selected)
        assertFalse(rows.first { it.option.id == 4L }.selected)
    }

    @Test
    fun `create is offered for a new name and not for an exact title or a blank query`() {
        assertTrue(canCreateFromQuery(options, "Таверна"))
        assertFalse(canCreateFromQuery(options, "мирдейл"))
        assertFalse(canCreateFromQuery(options, "   "))
    }

    @Test
    fun `an unavailable option is listed last and a covered one is inactive while its cover is checked`() {
        val tower = SelectOption(5, "Донжон", unavailableNote = "это само место")
        val hall = SelectOption(6, "Зал", coveredBy = listOf(SelectCover(1, "Мирдейл")))
        val rows = selectRows(options + tower + hall, "", setOf(1L, 6L))

        assertEquals(5L, rows.last().option.id)
        assertTrue(rows.last().isInactive)
        val covered = rows.first { it.option.id == 6L }
        assertEquals("Мирдейл", covered.carriedBy)
        assertFalse(covered.selected)
        assertEquals(setOf(1L), pickedIds(options + hall, setOf(1L, 6L)))
        assertEquals(setOf(6L), pickedIds(options + hall, setOf(6L)))
    }
}
