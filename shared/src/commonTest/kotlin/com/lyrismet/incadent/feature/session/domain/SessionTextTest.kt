package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.mentionToken
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTextTest {
    private val parts =
        SessionTextParts(
            heading = "Session I · Day one · 4 Mar",
            intro = "Lira unrolls a scroll.",
            notes =
                listOf(
                    SessionTextNote("18:00", "met @[Old_miller|n:1] at @[Mirdale|l:2]"),
                    SessionTextNote("18:30", "   "),
                    SessionTextNote("", "a plain line"),
                ),
            actorsTitle = "Cast",
            actors = listOf(SessionTextActors("NPC", listOf("Old miller")), SessionTextActors("Places", emptyList())),
        )

    @Test
    fun `the text has the heading the joke the notes and the cast in order`() {
        assertEquals(
            "Session I · Day one · 4 Mar\n\nLira unrolls a scroll.\n\n18:00 - met Old miller at Mirdale\na plain line\n\nCast\nNPC: Old miller",
            buildSessionText(parts),
        )
    }

    @Test
    fun `a mention loses its signs and a counted species keeps its count`() {
        val text =
            buildSessionText(
                parts.copy(
                    notes =
                        listOf(
                            SessionTextNote(
                                "",
                                "saw ${mentionToken(
                                    com.lyrismet.incadent.feature.mention.domain
                                        .MentionRef(EntityKind.SPECIES, 3),
                                    "Goblin",
                                    3,
                                )}",
                            ),
                        ),
                ),
            )

        assertTrue(text.contains("saw Goblin ×3"))
        assertFalse(text.contains("@["))
    }

    @Test
    fun `without a joke or a cast no blank lines are left`() {
        val text = buildSessionText(parts.copy(intro = null, actors = null, notes = emptyList()))

        assertEquals("Session I · Day one · 4 Mar", text)
    }

    @Test
    fun `an empty kind is not written`() {
        val text = buildSessionText(parts.copy(actors = listOf(SessionTextActors("Places", emptyList()))))

        assertFalse(text.contains("Cast"))
        assertFalse(text.contains("Places"))
    }

    @Test
    fun `the cast has a line per kind and counts a species met several times`() {
        val goblin = MentionEntity.SpeciesMention(3, "Goblin")
        val bodies =
            listOf(
                "two ${mentionToken(goblin.ref, "Goblin", 2)}",
                "one more ${mentionToken(goblin.ref, "Goblin", 1)}",
            )
        val actors =
            sessionActors(
                listOf(MentionEntity.PartyMention(1, "Lira"), MentionEntity.PartyMention(2, "Torvin"), goblin),
                bodies,
                listOf(EntityKind.SPECIES to "Species", EntityKind.PARTY to "Party", EntityKind.NPC to "NPC"),
            )

        assertEquals(listOf("Goblin ×3"), actors[0].names)
        assertEquals(listOf("Lira", "Torvin"), actors[1].names)
        assertTrue(actors[2].names.isEmpty())
    }

    @Test
    fun `a record gone from the codex is not part of the cast`() {
        val gone = MentionEntity.DeletedMention(9, "Ghost", EntityKind.NPC)

        assertTrue(sessionActors(listOf(gone), emptyList(), listOf(EntityKind.NPC to "NPC")).single().names.isEmpty())
    }

    @Test
    fun `the character name is put into the joke`() {
        assertEquals("Lira swears it is true.", introText("{name} swears it is true.", "Lira"))
        assertEquals("A tavern tale.", introText("A tavern tale.", null))
    }

    @Test
    fun `the joke is never the one told last time`() {
        repeat(200) { seed ->
            val next = nextIntroIndex(8, last = 3, random = Random(seed))
            assertTrue(next in 0 until 8)
            assertTrue(next != 3)
        }
    }

    @Test
    fun `any joke can come when none was told before`() {
        val seen = (0 until 200).map { nextIntroIndex(5, last = -1, random = Random(it)) }.toSet()

        assertEquals((0 until 5).toSet(), seen)
    }

    @Test
    fun `one joke alone is told again`() {
        assertEquals(0, nextIntroIndex(1, last = 0, random = Random(1)))
        assertEquals(0, nextIntroIndex(0, last = -1, random = Random(1)))
    }
}
