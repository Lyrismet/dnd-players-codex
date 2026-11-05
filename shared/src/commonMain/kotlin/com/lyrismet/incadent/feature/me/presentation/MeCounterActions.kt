package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_resource_deleted_title
import dndplayerscodex.shared.generated.resources.me_slot_name
import dndplayerscodex.shared.generated.resources.me_spells_hidden_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** every write of the resources and spell slots, the repository stays the one place that holds them */
internal class MeCounterActions(
    private val scope: CoroutineScope,
    private val resources: CharacterResourceRepository,
    private val sheets: CharacterSheetRepository,
    private val undoController: UndoController,
) {
    fun setCurrent(
        id: Long,
        value: Int,
    ) {
        scope.launch { resources.setCurrent(id, value) }
    }

    fun save(
        memberId: Long,
        draft: MeOverlay.Resource,
    ) {
        scope.launch {
            resources.upsert(
                memberId,
                CharacterResource(
                    id = draft.id,
                    name = draft.name.trim(),
                    max = draft.max,
                    current = if (draft.id == 0L) draft.max else draft.current,
                    position = draft.position,
                    originId = draft.originId,
                ),
            )
        }
    }

    // a level set to zero goes away, a new level starts full and a kept one keeps what is left
    fun saveSlots(
        memberId: Long,
        columns: List<SlotColumn>,
        maxima: Map<Int, Int>,
    ) {
        scope.launch {
            val stored = columns.associateBy { it.level }
            SPELL_LEVELS.forEach { level ->
                val max = maxima[level] ?: 0
                val existing = stored[level]?.resource
                when {
                    existing != null && max == 0 -> resources.delete(existing.id)
                    existing != null -> if (existing.max != max) resources.upsert(memberId, existing.copy(max = max))
                    max > 0 ->
                        resources.upsert(
                            memberId,
                            CharacterResource(
                                id = 0,
                                name = getString(Res.string.me_slot_name, level),
                                max = max,
                                current = max,
                                slotLevel = level,
                                position = level,
                            ),
                        )
                }
            }
        }
    }

    fun delete(
        memberId: Long,
        resource: CharacterResource,
    ) {
        scope.launch {
            resources.delete(resource.id)
            undoController.show(getString(Res.string.me_resource_deleted_title), resource.name) {
                resources.upsert(memberId, resource.copy(id = 0))
            }
        }
    }

    fun setSpellsHidden(
        member: PartyMember,
        hidden: Boolean,
    ) {
        scope.launch {
            sheets.updateSheet(member.id) { it.copy(spellsHidden = hidden) }
            if (hidden) {
                undoController.show(getString(Res.string.me_spells_hidden_title), member.name) {
                    sheets.updateSheet(member.id) { it.copy(spellsHidden = false) }
                }
            }
        }
    }
}
