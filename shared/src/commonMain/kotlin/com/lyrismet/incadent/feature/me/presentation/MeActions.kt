package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import com.lyrismet.incadent.feature.me.domain.hit
import com.lyrismet.incadent.feature.me.domain.next
import com.lyrismet.incadent.feature.me.domain.withDeathSaveDot
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_fallen_done_title
import dndplayerscodex.shared.generated.resources.me_not_mine_done_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** every write of the "me" screen, each one goes straight to the sheet or the party record */
internal class MeActions(
    private val scope: CoroutineScope,
    private val party: PartyRepository,
    private val sheets: CharacterSheetRepository,
    private val undoController: UndoController,
) {
    fun hit(
        memberId: Long,
        mode: HitPointMode,
        amount: Int,
    ) = vitals(memberId) { it.hit(mode, amount) }

    fun vitals(
        memberId: Long,
        change: (CharacterVitals) -> CharacterVitals,
    ) {
        scope.launch { sheets.updateVitals(memberId, change) }
    }

    fun successDot(
        memberId: Long,
        index: Int,
    ) = vitals(memberId) { it.copy(deathSavesSuccess = it.deathSavesSuccess.withDeathSaveDot(index)) }

    fun failureDot(
        memberId: Long,
        index: Int,
    ) = vitals(memberId) { it.copy(deathSavesFailure = it.deathSavesFailure.withDeathSaveDot(index)) }

    fun sheet(
        memberId: Long,
        change: (CharacterSheet) -> CharacterSheet,
    ) {
        scope.launch { sheets.updateSheet(memberId, change) }
    }

    suspend fun sheetNow(
        memberId: Long,
        change: (CharacterSheet) -> CharacterSheet,
    ) = sheets.updateSheet(memberId, change)

    fun toggleSave(
        memberId: Long,
        ability: Ability,
    ) = sheet(memberId) { it.copy(saves = it.saves.toggled(ability)) }

    fun nextMark(
        memberId: Long,
        skill: Skill,
    ) = sheet(memberId) { it.copy(skillMarks = it.skillMarks + (skill to it.mark(skill).next())) }

    /** a write of several steps one after the other, so none reads what another is still changing */
    fun write(block: suspend (PartyRepository, CharacterSheetRepository) -> Unit) {
        scope.launch { block(party, sheets) }
    }

    fun member(
        memberId: Long,
        change: (PartyMember) -> PartyMember,
    ) {
        scope.launch { party.getById(memberId)?.let { party.upsert(change(it)) } }
    }

    fun markFallen(member: PartyMember) {
        scope.launch {
            party.setFallen(member.id, true, null)
            undoController.show(getString(Res.string.me_fallen_done_title), member.name) {
                party.setFallen(member.id, false, null)
            }
        }
    }

    // a mark given to somebody else since stays with them when the undo comes
    fun releaseMark(member: PartyMember) {
        scope.launch {
            party.getById(member.id)?.let { party.upsert(it.copy(isPlayerCharacter = false)) }
            undoController.show(getString(Res.string.me_not_mine_done_title), member.name) {
                if (party.observeAll().first().none { it.isPlayerCharacter }) {
                    party.getById(member.id)?.let { party.upsert(it.copy(isPlayerCharacter = true)) }
                }
            }
        }
    }
}
