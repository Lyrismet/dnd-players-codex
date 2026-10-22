package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import kotlin.test.Test
import kotlin.test.assertEquals

class MentionTokenRemapTest {
    @Test
    fun `a known token moves to its new id`() {
        val out = remapMentionTokens("met @[Bob|n:12] at @[Inn|l:3]") { if (it.type == EntityKind.NPC) 40L else 5L }
        assertEquals("met @[Bob|n:40] at @[Inn|l:5]", out)
    }

    @Test
    fun `an unknown token becomes a gone record`() {
        assertEquals("@[Bob|n:0]", remapMentionTokens("@[Bob|n:12]") { null })
    }

    @Test
    fun `the species count stays`() {
        assertEquals("@[Wolf|s:7|3]", remapMentionTokens("@[Wolf|s:1|3]") { 7L })
    }

    @Test
    fun `plain text is left alone`() {
        assertEquals("no tokens @here", remapMentionTokens("no tokens @here") { 1L })
    }
}
