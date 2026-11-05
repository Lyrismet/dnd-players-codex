package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency

/** writes what an edit sheet collected, the keypad of the hit points has its own way in [MeActions.hit] */
internal fun MeActions.commit(
    member: PartyMember,
    overlay: MeOverlay,
) {
    val id = member.id
    when (overlay) {
        is MeOverlay.AbilityEdit ->
            sheet(id) {
                it.copy(
                    abilities = it.abilities + (overlay.ability to overlay.score),
                    saves = if (overlay.save) it.saves + overlay.ability else it.saves - overlay.ability,
                )
            }
        is MeOverlay.SkillEdit ->
            sheet(id) {
                it.copy(
                    skillMarks = it.skillMarks + (overlay.skill to overlay.proficiency),
                    skillAdjustments = it.skillAdjustments + (overlay.skill to overlay.adjustment),
                )
            }
        is MeOverlay.ArmorClass ->
            vitals(
                id,
            ) { it.copy(armorClass = overlay.value, armorClassFrom = overlay.from.trim()) }
        is MeOverlay.Speed -> sheet(id) { it.copy(speed = overlay.value) }
        is MeOverlay.Adjustment ->
            sheet(id) {
                if (overlay.tile == MeTile.INITIATIVE) {
                    it.copy(initiativeAdjustment = overlay.value)
                } else {
                    it.copy(passivePerceptionAdjustment = overlay.value)
                }
            }
        is MeOverlay.Fill -> commitFill(member, overlay.draft)
        is MeOverlay.EditCharacter -> commitEdit(id, overlay.draft)
        is MeOverlay.HitPoints,
        is MeOverlay.Slots,
        is MeOverlay.Resource,
        is MeOverlay.SpellAbility,
        is MeOverlay.Rest,
        is MeOverlay.Wallet,
        MeOverlay.Templates,
        -> Unit
    }
}

private fun MeActions.commitEdit(
    id: Long,
    draft: CharacterDraft,
) {
    write { party, sheets ->
        party.getById(id)?.let {
            party.upsert(
                it.copy(
                    name = draft.name.trim().ifBlank { it.name },
                    characterClass = draft.characterClass.trim(),
                    race = draft.race.trim(),
                    level = draft.level,
                    playerName = draft.playerName.trim(),
                ),
            )
        }
        sheets.updateSheet(id) {
            it.copy(
                subclass = draft.subclass.trim(),
                background = draft.background.trim(),
                alignment = draft.alignment.trim(),
            )
        }
    }
}

// a skipped step writes nothing, so a half filled sheet keeps what the player did not touch
private fun MeActions.commitFill(
    member: PartyMember,
    draft: FillDraft,
) {
    write { party, sheets ->
        val id = member.id
        if (FillDraft.STEP_CLASS !in draft.skipped) {
            party.getById(id)?.let {
                party.upsert(
                    it.copy(
                        characterClass = draft.characterClass.trim().ifBlank { it.characterClass },
                        level = draft.level,
                    ),
                )
            }
            sheets.updateSheet(id) { it.copy(subclass = draft.subclass.trim()) }
        }
        if (FillDraft.STEP_ABILITIES !in draft.skipped) sheets.updateSheet(id) { it.copy(abilities = draft.abilities) }
        if (FillDraft.STEP_SKILLS !in draft.skipped) {
            sheets.updateSheet(id) {
                it.copy(saves = draft.saves, skillMarks = draft.marks.filterValues { mark -> mark != Proficiency.NONE })
            }
        }
        if (FillDraft.STEP_NUMBERS !in draft.skipped) {
            party.getById(id)?.let { stored ->
                val max = draft.hpMax ?: stored.hpMax
                party.upsert(stored.copy(hpMax = max, hpCurrent = stored.hpCurrent ?: max))
            }
            sheets.updateVitals(id) { it.copy(armorClass = draft.armorClass) }
            sheets.updateSheet(id) { it.copy(speed = draft.speed) }
        }
    }
}
