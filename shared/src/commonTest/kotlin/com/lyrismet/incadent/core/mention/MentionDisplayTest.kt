package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals

private val ragnar = MentionEntity.NpcMention(12, "Рагнар Каменный", NpcStatus.FRIEND, NpcLifeState.ALIVE)

class MentionDisplayTest {
    private val raw = "ждали ${ragnar.token()} там"
    private val display = MentionDisplay(raw)

    @Test
    fun `a token is shown as its at name and the rest is untouched`() {
        assertEquals("ждали @Рагнар Каменный там", display.text)
    }

    @Test
    fun `a cursor before and after a token maps to the same place in the text`() {
        val tokenStart = raw.indexOf('@')
        val tokenEnd = tokenStart + ragnar.token().length

        assertEquals(6, display.toDisplay(tokenStart))
        assertEquals(display.text.indexOf(" там"), display.toDisplay(tokenEnd))
        assertEquals(tokenStart, display.toRaw(6))
        assertEquals(tokenEnd, display.toRaw(display.text.indexOf(" там")))
    }

    @Test
    fun `a cursor inside a shown tag snaps to the nearer end of the stored token`() {
        val tokenStart = raw.indexOf('@')
        val tokenEnd = tokenStart + ragnar.token().length

        assertEquals(tokenStart, display.toRaw(7))
        assertEquals(tokenEnd, display.toRaw(display.text.indexOf(" там") - 1))
    }

    @Test
    fun `text without a token maps one to one`() {
        val plain = MentionDisplay("просто текст")

        assertEquals("просто текст", plain.text)
        assertEquals(4, plain.toDisplay(4))
        assertEquals(4, plain.toRaw(4))
    }
}
