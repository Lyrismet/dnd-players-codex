package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.hp.HealedToMax
import com.lyrismet.incadent.core.hp.healToMaxToast
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.grouping.decodeFacts
import com.lyrismet.incadent.feature.codex.domain.grouping.encodeFacts
import com.lyrismet.incadent.feature.codex.domain.grouping.followersOf
import com.lyrismet.incadent.feature.codex.domain.grouping.withFact
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
import com.lyrismet.incadent.feature.me.domain.leveledUp
import com.lyrismet.incadent.feature.me.domain.missingHp
import com.lyrismet.incadent.feature.me.domain.next
import com.lyrismet.incadent.feature.me.domain.withDeathSaveDot
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.features_removed_title
import incadent.shared.generated.resources.me_level_up_toast
import incadent.shared.generated.resources.me_level_up_toast_max
import incadent.shared.generated.resources.me_level_up_toast_no_max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** every write of the "me" screen, each one goes straight to the sheet or the party record */
internal class MeActions(
    private val scope: CoroutineScope,
    private val party: PartyRepository,
    private val sheets: CharacterSheetRepository,
    private val undoController: UndoController,
    private val followsPartyLevel: suspend () -> Boolean,
) {
    fun hit(
        memberId: Long,
        mode: HitPointMode,
        amount: Int,
    ) = if (mode == HitPointMode.MAX) setMax(memberId, amount) else vitals(memberId) { it.hit(mode, amount) }

    // the maximum lives in the party record, the vitals write does not carry it
    private fun setMax(
        memberId: Long,
        amount: Int,
    ) {
        scope.launch {
            val member = party.getById(memberId) ?: return@launch
            val typed = CharacterVitals(member.hpMax, member.hpCurrent).hit(HitPointMode.MAX, amount)
            party.upsert(member.copy(hpMax = typed.hpMax, hpCurrent = typed.hpCurrent))
            party.setHpMaxApprox(memberId, false)
        }
    }

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

    /** levels up with the typed hit points for each, the undo puts the old level, maximum and hit points back */
    fun levelUp(
        member: PartyMember,
        gain: Int,
        levels: Int,
    ) {
        scope.launch {
            val before = party.getById(member.id) ?: return@launch
            val after = before.leveledUp(gain, levels) ?: return@launch
            val followers = if (followsPartyLevel()) party.followersOf(before, after) else emptyList()
            party.saveLevelUp(after, followers.map { it.copy(level = after.level) })
            val title = getString(Res.string.me_level_up_toast, before.name, after.level ?: 0)
            val subtitle =
                after.hpMax?.let { getString(Res.string.me_level_up_toast_max, it) }
                    ?: getString(Res.string.me_level_up_toast_no_max)
            undoController.show(title, subtitle) {
                // a follower the player moved on since stays where they are
                val back =
                    followers.mapNotNull { old ->
                        party.getById(old.id)?.takeIf { it.level == after.level }?.copy(
                            level = old.level,
                            hpMaxApprox = old.hpMaxApprox,
                        )
                    }
                party.restore(listOf(before) + back)
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

    /** writes the facts, a removed one is offered back on the undo bar */
    fun setFacts(
        memberId: Long,
        facts: List<String>,
    ) {
        scope.launch {
            val before = decodeFacts(sheets.getSheet(memberId).features)
            sheets.updateSheet(memberId) { it.copy(features = facts.encodeFacts()) }
            val removed = before.firstOrNull { it !in facts } ?: return@launch
            undoController.show(getString(Res.string.features_removed_title), removed) {
                val now = decodeFacts(sheets.getSheet(memberId).features)
                val restored = if (now == facts) before else now.withFact(removed)
                sheets.updateSheet(memberId) { it.copy(features = restored.encodeFacts()) }
            }
        }
    }

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
