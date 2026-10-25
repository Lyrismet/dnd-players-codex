package com.lyrismet.incadent.feature.session.presentation.tags

import com.lyrismet.incadent.feature.session.domain.model.BUILT_IN_SESSION_TAGS
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTagLabelsTest {
    @Test
    fun `every built-in tag has a label resource`() {
        assertEquals(BUILT_IN_SESSION_TAGS.size, BUILT_IN_SESSION_TAG_LABELS.size)
    }

    @Test
    fun `each built-in tag maps to its own label resource`() {
        val expected =
            mapOf(
                "Бой" to "session_tag_builtin_combat",
                "Сюжет" to "session_tag_builtin_plot",
                "Расследование" to "session_tag_builtin_investigation",
                "Интрига" to "session_tag_builtin_intrigue",
                "Торговля" to "session_tag_builtin_trade",
                "Путешествие" to "session_tag_builtin_travel",
                "Отдых" to "session_tag_builtin_rest",
            )
        val actual = BUILT_IN_SESSION_TAGS.zip(BUILT_IN_SESSION_TAG_LABELS.map { it.key }).toMap()
        assertEquals(expected, actual)
    }
}
