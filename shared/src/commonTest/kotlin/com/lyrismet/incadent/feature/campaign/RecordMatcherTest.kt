package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.LocalRecord
import com.lyrismet.incadent.feature.campaign.domain.exchange.RecordMatch
import com.lyrismet.incadent.feature.campaign.domain.exchange.matchRecord
import com.lyrismet.incadent.feature.campaign.domain.exchange.noteOverlap
import com.lyrismet.incadent.feature.campaign.domain.exchange.sessionKey
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RecordMatcherTest {
    private val locals =
        listOf(
            LocalRecord(1, "Bob", "bob-id"),
            LocalRecord(2, "Деревня Кахури", "village-id"),
        )

    @Test
    fun `the origin id wins over a different name`() {
        assertEquals(RecordMatch.ByOrigin(1), matchRecord("bob-id", "Robert", locals))
    }

    @Test
    fun `an alias leads to the survivor`() {
        val match = matchRecord("old-id", "Whoever", locals) { if (it == "old-id") "village-id" else it }
        assertEquals(RecordMatch.ByOrigin(2), match)
    }

    @Test
    fun `the same name with another id is an exact twin`() {
        assertEquals(RecordMatch.ByName(1), matchRecord("other", "  bob ", locals))
    }

    @Test
    fun `a name without an id matches by name`() {
        assertEquals(RecordMatch.ByName(1), matchRecord(null, "BOB", locals))
    }

    @Test
    fun `a name inside another is only similar`() {
        assertEquals(RecordMatch.Similar(2, "Деревня Кахури"), matchRecord("x", "Кахури", locals))
    }

    @Test
    fun `a pair told to be different is not offered again`() {
        val apart = setOf("x" to "village-id")
        assertEquals(RecordMatch.None, matchRecord("x", "Кахури", locals, apart))
    }

    @Test
    fun `a very short name is never similar`() {
        assertEquals(RecordMatch.None, matchRecord("x", "Bo", locals))
    }

    @Test
    fun `an unrelated name is new`() {
        assertEquals(RecordMatch.None, matchRecord("x", "Eve", locals))
    }

    @Test
    fun `a session key joins the day and the folded title`() {
        val day = LocalDate(2026, 10, 24)
        assertEquals(sessionKey(day, "Огни на холме"), sessionKey(day, " огни на холме "))
        assertNotEquals(sessionKey(day, "Огни"), sessionKey(LocalDate(2026, 10, 25), "Огни"))
    }

    @Test
    fun `six notes and four with two shared make eight`() {
        val mine = listOf("a", "b", "m1", "m2", "m3", "m4")
        val theirs = listOf("a", "b", "t1", "t2")
        val overlap = noteOverlap(mine, theirs)
        assertEquals(2, overlap.common)
        assertEquals(8, overlap.merged)
    }

    @Test
    fun `notes without an id never count as shared`() {
        assertEquals(0, noteOverlap(listOf("", ""), listOf("", "")).common)
    }
}
