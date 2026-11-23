package com.lyrismet.incadent.feature.codex.domain.spellimport

/** the sites a spell can be imported from, [analyticsKey] is what the usage event reports */
enum class SpellSource(
    val analyticsKey: String,
    val siteName: String,
    val english: Boolean = false,
) {
    DND_SU("dnd_su", "dnd.su"),
    NEXT_DND_SU("next_dnd_su", "next.dnd.su"),
    OPEN5E("open5e", "Open5e", english = true),
    DND5E_API("dnd5eapi", "dnd5eapi.co", english = true),
}
