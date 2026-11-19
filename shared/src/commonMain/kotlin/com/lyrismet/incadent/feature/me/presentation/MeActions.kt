package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.hp.HealedToMax
import com.lyrismet.incadent.core.hp.healToMaxToast
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import com.lyrismet.incadent.feature.me.domain.currentHp
import com.lyrismet.incadent.feature.me.domain.hit
import com.lyrismet.incadent.feature.me.domain.missingHp
import com.lyrismet.incadent.feature.me.domain.next
import com.lyrismet.incadent.feature.me.domain.withDeathSaveDot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    /** all the hit points back, the temporary ones stay, the undo puts the old hit points and death saves back */
    fun healToMax(
        member: PartyMember,
        before: CharacterVitals,
    ) {
        val missing = before.missingHp?.takeIf { it > 0 } ?: return
        hit(member.id, HitPointMode.HEAL, missing)
        scope.launch {
            val max = before.hpMax ?: return@launch
            val (title, subtitle) =
                healToMaxToast(listOf(HealedToMax(member.name, before.currentHp, max, before.tempHp)))
            undoController.show(title, subtitle) {
                sheets.updateVitals(member.id) {
                    it.copy(
                        hpCurrent = before.hpCurrent,
                        deathSavesSuccess = before.deathSavesSuccess,
                        deathSavesFailure = before.deathSavesFailure,
                    )
                }
            }
        }
    }

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

    /** one tap on a shield, the record is the one the party card shows */
    fun shield(
        memberId: Long,
        type: DamageType,
        state: DamageState?,
    ) {
        scope.launch { party.setVulnerability(memberId, type, state) }
    }

    fun member(
        memberId: Long,
        change: (PartyMember) -> PartyMember,
    ) {
        scope.launch { party.getById(memberId)?.let { party.upsert(change(it)) } }
    }

    fun markFallen(member: PartyMember) {
        scope.launch { party.setFallen(member.id, true, null) }
    }

    fun releaseMark(member: PartyMember) {
        scope.launch { party.getById(member.id)?.let { party.upsert(it.copy(isPlayerCharacter = false)) } }
    }
}
