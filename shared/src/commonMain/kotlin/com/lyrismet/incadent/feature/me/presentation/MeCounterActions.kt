package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_resource_deleted_title
import dndplayerscodex.shared.generated.resources.me_slot_name
import dndplayerscodex.shared.generated.resources.me_spells_hidden_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** every write of the resources and spell slots, the repository stays the one place that holds them */
internal class MeCounterActions(
    private val scope: CoroutineScope,
    private val resources: CharacterResourceRepository,
    private val sheets: CharacterSheetRepository,
    private val undoController: UndoController,
) {
    fun pressCell(
        id: Long,
        index: Int,
    ) {
        scope.launch { resources.pressCell(id, index) }
    }

    fun save(
        memberId: Long,
        draft: MeOverlay.Resource,
    ) {
        scope.launch {
            if (draft.id == 0L) {
                resources.upsert(
                    memberId,
                    CharacterResource(
                        id = 0,
                        name = draft.name.trim(),
                        max = draft.max,
                        current = draft.max,
                        position = draft.position,
                        originId = draft.originId,
                    ),
                )
            } else {
                resources.edit(draft.id, draft.name.trim(), draft.max)
            }
        }
    }

    // a level set to zero goes away, a new level starts full and a kept one keeps what is left
    fun saveSlots(
        memberId: Long,
        maxima: Map<Int, Int>,
    ) {
        scope.launch {
            // read fresh, a slot row with no max is not a column but still counts as the level's row
            val stored =
                resources
                    .observe(memberId)
                    .first()
                    .filter { it.isSpellSlot }
                    .associateBy { it.slotLevel }
            SPELL_LEVELS.forEach { level ->
                val max = maxima[level] ?: 0
                val existing = stored[level]
                when {
                    existing != null && max == 0 -> resources.delete(existing.id)
                    existing != null -> if (existing.max != max) resources.edit(existing.id, existing.name, max)
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
