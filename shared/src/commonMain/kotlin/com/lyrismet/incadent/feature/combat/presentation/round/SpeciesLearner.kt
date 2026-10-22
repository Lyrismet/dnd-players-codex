package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository

/** the two lines of the bar that offers to take a change back */
internal data class UndoNote(
    val title: String,
    val subtitle: String,
)

/** writes what a fight taught about a species' damage into the bestiary, and offers to take it back */
internal class SpeciesLearner(
    private val speciesRepository: SpeciesRepository,
    private val undo: UndoController,
) {
    /** only the offer to take back, for a change that wrote nothing to the bestiary */
    fun offerUndo(
        note: UndoNote,
        onUndo: suspend () -> Unit,
    ) = undo.show(note.title, note.subtitle, onUndo)

    suspend fun record(
        speciesId: Long,
        type: DamageType,
        state: DamageState,
        note: UndoNote,
        alsoUndo: suspend () -> Unit = {},
    ) {
        val species = speciesRepository.getById(speciesId) ?: return
        val before = species.damage[type]
        speciesRepository.upsert(species.copy(damage = species.damage + (type to state)))
        undo.show(note.title, note.subtitle) {
            speciesRepository.getById(speciesId)?.let { current ->
                val restored = if (before == null) current.damage - type else current.damage + (type to before)
                speciesRepository.upsert(current.copy(damage = restored))
            }
            alsoUndo()
        }
    }
}
