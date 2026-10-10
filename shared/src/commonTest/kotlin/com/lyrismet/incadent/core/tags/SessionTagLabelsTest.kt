package com.lyrismet.incadent.core.tags

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTagLabelsTest {
    @Test
    fun `every built-in tag has a label resource`() {
        assertEquals(BUILT_IN_SESSION_TAGS.size, BUILT_IN_SESSION_TAG_LABELS.size)
    }
}
