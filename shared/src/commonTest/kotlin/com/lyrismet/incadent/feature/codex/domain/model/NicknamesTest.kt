package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class NicknamesTest {
    @Test
    fun `nicknames are split on commas and trimmed`() {
        assertEquals(listOf("Рыжий", "Старик"), parseNicknames(" Рыжий ,Старик "))
    }

    @Test
    fun `blank and repeated nicknames are dropped`() {
        assertEquals(listOf("Рыжий"), parseNicknames("Рыжий, , Рыжий,"))
    }

    @Test
    fun `an empty text has no nicknames`() {
        assertEquals(emptyList(), parseNicknames(""))
    }

    @Test
    fun `nicknames survive a join and parse round trip`() {
        val nicknames = listOf("Рыжий", "Старик Хо")
        assertEquals(nicknames, parseNicknames(nicknames.joinNicknames()))
    }
}
