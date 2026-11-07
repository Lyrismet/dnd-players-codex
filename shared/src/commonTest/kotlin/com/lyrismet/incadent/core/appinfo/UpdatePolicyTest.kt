package com.lyrismet.incadent.core.appinfo

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatePolicyTest {
    private fun v(raw: String) = checkNotNull(VersionNumber.parse(raw))

    @Test
    fun `a well formed version is parsed`() {
        assertEquals(VersionNumber(9, 0, 0), VersionNumber.parse("9.0.0"))
        assertEquals(VersionNumber(0, 8, 1), VersionNumber.parse(" 0.8.1 "))
    }

    @Test
    fun `broken strings are not parsed`() {
        listOf("", "9", "9.0", "a.b.c", "1.-2.3", "1..3", "1.2.x").forEach {
            assertNull(VersionNumber.parse(it), it)
        }
    }

    @Test
    fun `a fourth number is the revision and a missing one is zero`() {
        assertEquals(VersionNumber(0, 9, 0, 1), VersionNumber.parse("0.9.0.1"))
        assertEquals(VersionNumber.parse("0.9.0"), VersionNumber.parse("0.9.0.0"))
        assertTrue(v("0.9.0.1") > v("0.9.0"))
        assertTrue(v("0.9.1") > v("0.9.0.9"))
        assertEquals("0.9.0.1", v("0.9.0.1").toString())
        assertNull(VersionNumber.parse("0.9.0.1.2"))
    }

    @Test
    fun `versions compare by major then minor then patch`() {
        assertTrue(v("1.0.0") > v("0.9.9"))
        assertTrue(v("0.10.0") > v("0.9.0"))
        assertTrue(v("0.9.1") > v("0.9.0"))
        assertEquals(0, v("0.9.0").compareTo(v("0.9.0")))
    }

    @Test
    fun `a version prints as dotted numbers`() {
        assertEquals("9.0.0", v("9.0.0").toString())
    }

    @Test
    fun `any threshold offers every newer version`() {
        val policy = UpdatePolicy(UpdateThreshold.ANY)
        assertTrue(policy.isWorthOffering(v("0.9.0"), v("0.9.1")))
        assertFalse(policy.isWorthOffering(v("0.9.1"), v("0.9.1")))
        assertFalse(policy.isWorthOffering(v("0.9.1"), v("0.9.0")))
    }

    @Test
    fun `minor threshold skips patch only gaps`() {
        val policy = UpdatePolicy(UpdateThreshold.MINOR)
        assertFalse(policy.isWorthOffering(v("0.9.0"), v("0.9.5")))
        assertTrue(policy.isWorthOffering(v("0.9.0"), v("0.10.0")))
        assertTrue(policy.isWorthOffering(v("0.9.0"), v("1.0.0")))
    }

    @Test
    fun `major threshold offers only a higher major`() {
        val policy = UpdatePolicy(UpdateThreshold.MAJOR)
        assertFalse(policy.isWorthOffering(v("1.0.0"), v("1.9.9")))
        assertTrue(policy.isWorthOffering(v("1.0.0"), v("2.0.0")))
    }

    @Test
    fun `status is available when the gap passes the threshold`() {
        assertEquals(UpdateStatus.Available(v("9.1.0")), UpdatePolicy().statusOf(v("9.0.0"), v("9.1.0")))
    }

    @Test
    fun `status is up to date for an equal or older latest version`() {
        assertEquals(UpdateStatus.UpToDate, UpdatePolicy().statusOf(v("9.0.0"), v("9.0.0")))
        assertEquals(UpdateStatus.UpToDate, UpdatePolicy().statusOf(v("9.0.0"), v("8.0.0")))
    }

    @Test
    fun `status is unknown when a version is missing`() {
        assertEquals(UpdateStatus.Unknown, UpdatePolicy().statusOf(v("9.0.0"), null))
        assertEquals(UpdateStatus.Unknown, UpdatePolicy().statusOf(null, v("9.0.0")))
    }

    @Test
    fun `a source with no answer leaves the status unknown`() =
        runTest {
            val source =
                object : LatestVersionSource {
                    override suspend fun latest(): VersionNumber? = null
                }
            assertEquals(UpdateStatus.Unknown, UpdatePolicy().statusOf(v("9.0.0"), source.latest()))
        }
}
