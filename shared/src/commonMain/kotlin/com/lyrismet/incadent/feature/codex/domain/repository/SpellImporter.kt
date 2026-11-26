package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell

/** why a link gave no spell */
enum class SpellImportFailure { UNSUPPORTED_SITE, OFFLINE, NOT_FOUND, UNREADABLE }

sealed interface SpellImportResult {
    data class Imported(
        val spell: ImportedSpell,
    ) : SpellImportResult

    data class Failed(
        val failure: SpellImportFailure,
    ) : SpellImportResult
}

/** turns a pasted link into a spell read from the site it points to, nothing is stored */
interface SpellImporter {
    suspend fun import(link: String): SpellImportResult
}
