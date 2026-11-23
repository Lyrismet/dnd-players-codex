package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpellLinkTest {
    private fun request(input: String): SpellRequest = assertIs<LinkParse.Request>(parseSpellLink(input)).request

    @Test
    fun `dnd su page becomes a page request`() {
        val request = request("https://dnd.su/spells/1-hellish-rebuke/")

        assertEquals(SpellSource.DND_SU, request.source)
        assertEquals(SpellFormat.DND_SU_HTML, request.format)
        assertEquals("https://dnd.su/spells/1-hellish-rebuke/", request.url)
    }

    @Test
    fun `other hosts of the site keep their host`() {
        assertEquals("https://5e14.dnd.su/spells/1-x/", request("https://5e14.dnd.su/spells/1-x/").url)
        assertEquals(SpellSource.NEXT_DND_SU, request("https://next.dnd.su/spells/1-x/").source)
        assertEquals(SpellSource.DND_SU, request("https://5e14.dnd.su/spells/1-x/").source)
        assertEquals(
            "https://next.dnd.su/spells/10669-tashas-bubbling-cauldron/",
            request("next.dnd.su/spells/10669-tashas-bubbling-cauldron").url,
        )
    }

    @Test
    fun `a link without a scheme and with spaces and www is made normal`() {
        assertEquals("https://dnd.su/spells/1-x/", request("  www.dnd.su/spells/1-x?tab=1#top \n").url)
    }

    @Test
    fun `plain http is made https`() {
        assertEquals("https://dnd.su/spells/1-x/", request("http://dnd.su/spells/1-x").url)
    }

    @Test
    fun `open5e page maps to the v1 api by its slug`() {
        val request = request("https://open5e.com/spells/fireball")

        assertEquals(SpellFormat.OPEN5E_V1, request.format)
        assertEquals("https://api.open5e.com/v1/spells/fireball/", request.url)
    }

    @Test
    fun `open5e page with a document key maps to the v2 api`() {
        val request = request("https://open5e.com/spells/srd_antimagic-field")

        assertEquals(SpellFormat.OPEN5E_V2, request.format)
        assertEquals("https://api.open5e.com/v2/spells/srd_antimagic-field/", request.url)
    }

    @Test
    fun `open5e api addresses keep their version and default to v2`() {
        assertEquals(SpellFormat.OPEN5E_V1, request("https://api.open5e.com/v1/spells/fireball/").format)
        assertEquals(
            "https://api.open5e.com/v2/spells/srd-2024_fireball/",
            request("api.open5e.com/v2/spells/srd-2024_fireball").url,
        )
        assertEquals(SpellFormat.OPEN5E_V2, request("https://api.open5e.com/spells/fireball/").format)
    }

    @Test
    fun `dnd5eapi address gets the www host and a year`() {
        assertEquals(
            "https://www.dnd5eapi.co/api/2014/spells/fireball",
            request("https://www.dnd5eapi.co/api/spells/fireball").url,
        )
        assertEquals(
            "https://www.dnd5eapi.co/api/2024/spells/fireball",
            request("dnd5eapi.co/api/2024/spells/fireball").url,
        )
    }

    @Test
    fun `another site is not supported`() {
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("https://roll20.net/compendium/dnd5e/Fireball"))
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("ftp://dnd.su/spells/1-x/"))
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("not a link"))
    }

    @Test
    fun `a host that only looks like a known one is not supported`() {
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("https://dnd.su.evil.example/spells/1-x/"))
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("https://dnd.su@evil.example/spells/1-x/"))
        assertEquals(LinkParse.UnsupportedSite, parseSpellLink("https://dnd.su:8080/spells/1-x/"))
    }

    @Test
    fun `a known site with another kind of page is not a spell`() {
        assertEquals(LinkParse.NotASpell, parseSpellLink("https://dnd.su/bestiary/12-goblin/"))
        assertEquals(LinkParse.NotASpell, parseSpellLink("https://dnd.su/spells/"))
        assertEquals(LinkParse.NotASpell, parseSpellLink("https://www.dnd5eapi.co/api/monsters/goblin"))
    }
}
