package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.grouping.duplicateNpcOf
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.repository.NpcMergeRepository
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.presentation.form.ClassSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.NpcFormMerge
import com.lyrismet.incadent.feature.codex.presentation.form.npcMergeTexts
import com.lyrismet.incadent.feature.codex.presentation.summary.entityWriteLock
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.RoleParts
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_name_plain_sub
import incadent.shared.generated.resources.combat_name_plain_title
import incadent.shared.generated.resources.combat_rename_sub
import incadent.shared.generated.resources.combat_rename_title
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/** what is open or typed about the identity of the chosen participant */
internal data class IdentityDraft(
    val memberId: Long,
    val rename: String? = null,
    val focus: Boolean = false,
    val classOpen: Boolean = false,
    val linkOpen: Boolean = false,
)

/** renames a participant tied to an npc, sets the role of one that is only in the fight, opens the npc list */
@Suppress("LongParameterList")
internal class IdentityHandler(
    private val editor: RoundEditor,
    private val selected: MutableState<Long?>,
    private val tab: MutableState<PanelTab>,
    private val draft: MutableState<IdentityDraft?>,
    private val npcs: NpcRepository,
    merge: NpcMergeRepository,
    private val combats: CombatRepository,
    private val undo: UndoController,
) {
    private val formMerge = NpcFormMerge(npcs, merge, undo, ::npcMergeTexts)

    fun onEvent(
        event: CombatRoundEvent,
        known: List<Npc>,
    ) {
        val id = selected.value ?: return
        when (event) {
            CombatRoundEvent.NameTapped -> {
                tab.value = PanelTab.MORE
                draft.value = IdentityDraft(id, focus = true)
            }
            is CombatRoundEvent.RenameChanged -> update(id) { copy(rename = event.text) }
            CombatRoundEvent.RenameConfirmed -> confirmRename(id, known)
            CombatRoundEvent.MergeConfirmed -> mergeInto(id)
            CombatRoundEvent.ClassOpened -> update(id) { copy(classOpen = true) }
            is CombatRoundEvent.ClassChosen -> {
                update(id) { copy(classOpen = false) }
                editor.withMember(id) { _, member -> setClass(member, event.characterClass, event.subclass) }
            }
            CombatRoundEvent.ClassClosed -> update(id) { copy(classOpen = false) }
            CombatRoundEvent.LinkOpened -> update(id) { copy(linkOpen = true) }
            CombatRoundEvent.LinkClosed -> update(id) { copy(linkOpen = false) }
            else -> Unit
        }
    }

    private fun current(memberId: Long) = draft.value?.takeIf { it.memberId == memberId } ?: IdentityDraft(memberId)

    private fun update(
        memberId: Long,
        change: IdentityDraft.() -> IdentityDraft,
    ) {
        draft.value = current(memberId).change()
    }

    // a name that a record of the codex already has is not taken, the button under the field is the way on
    private fun confirmRename(
        memberId: Long,
        known: List<Npc>,
    ) {
        val typed = current(memberId).rename?.trim().orEmpty()
        editor.withMember(memberId) { _, member ->
            val before = member.npcId?.let { npcs.getById(it) }
            if (duplicateNpcOf(typed, known, before?.id) == null) {
                update(memberId) { copy(rename = null, focus = false) }
                if (typed.isNotEmpty() && typed != (before?.name ?: member.name)) {
                    if (before != null) renameNpc(before, typed) else relabel(member, typed)
                }
            }
        }
    }

    private suspend fun renameNpc(
        before: Npc,
        typed: String,
    ) {
        entityWriteLock.withLock { npcs.upsert(before.copy(name = typed)) }
        undo.show(
            getString(Res.string.combat_rename_title, before.name, typed),
            getString(Res.string.combat_rename_sub),
        ) {
            entityWriteLock.withLock { npcs.getById(before.id)?.let { npcs.upsert(it.copy(name = before.name)) } }
        }
    }

    // a record of the codex keeps the class in its card, a label keeps it in the fight
    private suspend fun setClass(
        member: Combatant,
        characterClass: String,
        subclass: String,
    ) {
        val npc = member.npcId?.let { npcs.getById(it) }
        if (npc != null) {
            entityWriteLock.withLock { npcs.upsert(npc.copy(characterClass = characterClass, subclass = subclass)) }
        } else {
            combats.setRole(member.id, RoleParts.toStored(characterClass, subclass))
        }
    }

    // a label stays in this fight, the codex is not touched
    private suspend fun relabel(
        member: Combatant,
        typed: String,
    ) {
        combats.setLabel(member.id, typed, true)
        undo.showRelabel(combats, member, typed)
    }

    // the record that has the typed name stays, the one of this participant goes into it
    private fun mergeInto(memberId: Long) {
        val typed = current(memberId).rename.orEmpty()
        update(memberId) { copy(rename = null, focus = false) }
        editor.withMember(memberId) { _, member ->
            member.npcId?.let { formMerge.run(it, typed) }
        }
    }
}

internal fun IdentityDraft?.classSelect(
    memberId: Long,
    parts: RoleParts,
): ClassSelectState? =
    if (this == null ||
        this.memberId != memberId ||
        !classOpen
    ) {
        null
    } else {
        ClassSelectState(parts.characterClass, parts.subclass)
    }

/** tells that the participant is called [typed] in this fight only, the bar puts the old name back */
internal suspend fun UndoController.showRelabel(
    combats: CombatRepository,
    member: Combatant,
    typed: String,
) = show(
    getString(Res.string.combat_name_plain_title, member.name, typed),
    getString(Res.string.combat_name_plain_sub),
) { combats.setLabel(member.id, member.name, member.isLabel) }
