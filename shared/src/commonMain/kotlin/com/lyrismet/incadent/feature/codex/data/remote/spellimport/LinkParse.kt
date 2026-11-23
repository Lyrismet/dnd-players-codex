package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource

/** what a pasted text turns out to be */
internal sealed interface LinkParse {
    data class Request(
        val request: SpellRequest,
    ) : LinkParse

    /** the site is not one of the supported ones */
    data object UnsupportedSite : LinkParse

    /** the site is supported but the address is not the one of a spell */
    data object NotASpell : LinkParse
}

private val Address = Regex("^https://([^/?#]+)([^?#]*)")
private val Slug = Regex("^[A-Za-z0-9_-]+$")
private const val SPELLS = "spells"
private const val HTTPS = "https://"
private const val HTTP = "http://"

// the 2014 and the 2024 editions of the site share the markup of a spell page
private val DndSuHosts = setOf("dnd.su", "5e14.dnd.su", "next.dnd.su")

/** the hosts a request may go to or be redirected to */
internal val AllowedHosts = DndSuHosts + setOf("api.open5e.com", "www.dnd5eapi.co", "dnd5eapi.co")

private class LinkAddress(
    val host: String,
    val path: List<String>,
)

/** reads a pasted link: spaces dropped, a missing or plain http scheme made https, `www.` ignored, only known sites */
internal fun parseSpellLink(input: String): LinkParse {
    val address = addressOf(input) ?: return LinkParse.UnsupportedSite
    return when (address.host) {
        in DndSuHosts -> dndSu(address.host, address.path)
        "open5e.com" -> open5ePage(address.path)
        "api.open5e.com" -> open5eApi(address.path)
        "dnd5eapi.co" -> dnd5eApi(address.path)
        else -> LinkParse.UnsupportedSite
    }
}

private fun addressOf(input: String): LinkAddress? {
    val text = input.filterNot { it.isWhitespace() }
    val withScheme =
        when {
            text.startsWith(HTTPS, ignoreCase = true) -> HTTPS + text.substring(HTTPS.length)
            text.startsWith(HTTP, ignoreCase = true) -> HTTPS + text.substring(HTTP.length)
            "://" in text -> null
            else -> HTTPS + text
        }
    val groups = withScheme?.let { Address.find(it)?.groupValues } ?: return null
    val host = groups[1].lowercase().removePrefix("www.")
    // a port or a login in the host is how a link is made to look like another site
    return host
        .takeIf { name -> name.none { it == ':' || it == '@' } }
        ?.let { LinkAddress(it, groups[2].split('/').filter(String::isNotEmpty)) }
}

// the slug of "spells/<slug>", null for any other tail
private fun spellSlug(tail: List<String>): String? =
    tail.takeIf { it.size == 2 && it[0] == SPELLS }?.get(1)?.takeIf { Slug.matches(it) }

private fun request(
    source: SpellSource,
    format: SpellFormat,
    url: String?,
): LinkParse = url?.let { LinkParse.Request(SpellRequest(source, format, it)) } ?: LinkParse.NotASpell

private fun dndSu(
    host: String,
    path: List<String>,
): LinkParse {
    val source = if (host == "next.dnd.su") SpellSource.NEXT_DND_SU else SpellSource.DND_SU
    return request(source, SpellFormat.DND_SU_HTML, spellSlug(path)?.let { "https://$host/spells/$it/" })
}

// the page of a spell, a slug like "srd_fireball" is a v2 api key, a plain one is a v1 slug
private fun open5ePage(path: List<String>): LinkParse {
    val slug = spellSlug(path.take(2))
    val v2 = slug != null && '_' in slug
    return request(
        SpellSource.OPEN5E,
        if (v2) SpellFormat.OPEN5E_V2 else SpellFormat.OPEN5E_V1,
        slug?.let { "https://api.open5e.com/${if (v2) "v2" else "v1"}/spells/$it/" },
    )
}

// an address without a version is the newest one, like in the api itself
private fun open5eApi(path: List<String>): LinkParse {
    val version = path.firstOrNull()?.takeIf { it == "v1" || it == "v2" }
    val slug = spellSlug(if (version != null) path.drop(1) else path)
    val format = if (version == "v1") SpellFormat.OPEN5E_V1 else SpellFormat.OPEN5E_V2
    return request(SpellSource.OPEN5E, format, slug?.let { "https://api.open5e.com/${version ?: "v2"}/spells/$it/" })
}

// the site has no page of a spell, only the api, an address without a year is the 2014 rules
private fun dnd5eApi(path: List<String>): LinkParse {
    val rest = path.takeIf { it.firstOrNull() == "api" }?.drop(1)
    val year = rest?.firstOrNull()?.takeIf { it == "2014" || it == "2024" }
    val slug = rest?.let { spellSlug(if (year != null) it.drop(1) else it) }
    return request(
        SpellSource.DND5E_API,
        SpellFormat.DND5E_API,
        slug?.let { "https://www.dnd5eapi.co/api/${year ?: "2014"}/spells/$it" },
    )
}
