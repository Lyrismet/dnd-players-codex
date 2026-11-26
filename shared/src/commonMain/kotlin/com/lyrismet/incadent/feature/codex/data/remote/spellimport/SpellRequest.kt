package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource

/** how the answer of a source is read */
internal enum class SpellFormat { DND_SU_HTML, OPEN5E_V1, OPEN5E_V2, DND5E_API }

/** the address to fetch and the way to read what comes back */
internal data class SpellRequest(
    val source: SpellSource,
    val format: SpellFormat,
    val url: String,
)

/** reads the answer of a source by its format */
internal fun readSpell(
    format: SpellFormat,
    body: String,
): ImportedSpell? =
    when (format) {
        SpellFormat.DND_SU_HTML -> readDndSuSpell(body)
        SpellFormat.OPEN5E_V1 -> readOpen5eV1Spell(body)
        SpellFormat.OPEN5E_V2 -> readOpen5eV2Spell(body)
        SpellFormat.DND5E_API -> readDnd5eApiSpell(body)
    }
