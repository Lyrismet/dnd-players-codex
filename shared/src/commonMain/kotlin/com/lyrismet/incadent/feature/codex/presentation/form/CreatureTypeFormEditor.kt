package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** the creature type of the species form - picking one, and making, renaming and deleting the player's own */
internal class CreatureTypeFormEditor(
    private val fields: MutableState<CodexEntryFormFields?>,
    private val speciesRepository: SpeciesRepository,
) {
    fun onChanged(type: SpeciesType) {
        fields.value = fields.value?.copy(speciesType = type, creatureTypeSelectOpen = false)
    }

    fun onSelectToggled(isOpen: Boolean) {
        fields.value = fields.value?.copy(creatureTypeSelectOpen = isOpen)
    }

    // a type made in the sheet is taken by the form at once
    fun onCreated(
        scope: CoroutineScope,
        name: String,
    ) {
        if (name.isBlank()) return
        scope.launch { onChanged(SpeciesType.Custom(speciesRepository.createCustomType(name))) }
    }

    // the form keeps the type it picked, so the new name is read back
    fun onRenamed(
        scope: CoroutineScope,
        id: Long,
        name: String,
    ) {
        scope.launch {
            speciesRepository.renameCustomType(id, name)
            val picked = fields.value?.speciesType as? SpeciesType.Custom
            if (picked?.type?.id == id) {
                val renamed = speciesRepository.observeCustomTypes().first().find { it.id == id }
                if (renamed != null) fields.value = fields.value?.copy(speciesType = SpeciesType.Custom(renamed))
            }
        }
    }

    fun onDeleted(
        scope: CoroutineScope,
        id: Long,
    ) {
        scope.launch {
            speciesRepository.deleteCustomType(id)
            val picked = fields.value?.speciesType as? SpeciesType.Custom
            if (picked?.type?.id == id) fields.value = fields.value?.copy(speciesType = SpeciesType.None)
        }
    }
}
