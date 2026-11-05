package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.SheetRanges
import com.lyrismet.incadent.feature.me.domain.next

/** what an edit sheet shows after [event], a sheet that does not take the event stays as it is */
internal fun MeOverlay.reduce(event: OverlayEvent): MeOverlay =
    when (this) {
        is MeOverlay.AbilityEdit -> reduceAbility(event)
        is MeOverlay.SkillEdit -> reduceSkill(event)
        is MeOverlay.ArmorClass -> reduceArmorClass(event)
        is MeOverlay.Speed ->
            if (event is OverlayEvent.NumberStepped) {
                copy(
                    value = value.stepped(event.delta, SheetRanges.speed),
                )
            } else {
                this
            }
        is MeOverlay.Adjustment ->
            if (event is OverlayEvent.NumberStepped) {
                copy(
                    value = value.stepped(event.delta, SheetRanges.adjustment),
                )
            } else {
                this
            }
        is MeOverlay.Fill -> copy(draft = draft.reduce(event))
        is MeOverlay.EditCharacter -> reduceEdit(event)
        is MeOverlay.Slots -> reduceSlots(event)
        is MeOverlay.Resource -> reduceResource(event)
        is MeOverlay.HitPoints, is MeOverlay.SpellAbility, MeOverlay.Templates -> this
    }

private fun MeOverlay.AbilityEdit.reduceAbility(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.NumberStepped ->
            copy(
                sheet =
                    sheet.copy(
                        abilities = sheet.abilities + (ability to score.stepped(event.delta, SheetRanges.ability)),
                    ),
            )
        OverlayEvent.SaveToggled ->
            copy(sheet = sheet.copy(saves = sheet.saves.toggled(ability)))
        else -> this
    }

private fun MeOverlay.SkillEdit.reduceSkill(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.ProficiencyChosen ->
            copy(sheet = sheet.copy(skillMarks = sheet.skillMarks + (skill to event.proficiency)))
        is OverlayEvent.NumberStepped ->
            copy(
                sheet =
                    sheet.copy(
                        skillAdjustments =
                            sheet.skillAdjustments +
                                (skill to adjustment.stepped(event.delta, SheetRanges.adjustment)),
                    ),
            )
        OverlayEvent.AdjustmentCleared -> copy(sheet = sheet.copy(skillAdjustments = sheet.skillAdjustments - skill))
        else -> this
    }

private fun MeOverlay.ArmorClass.reduceArmorClass(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.NumberStepped -> copy(value = value.stepped(event.delta, SheetRanges.armorClass))
        is OverlayEvent.TextChanged ->
            if (event.field == OverlayField.ARMOR_CLASS_FROM) copy(from = event.value) else this
        else -> this
    }

private fun MeOverlay.EditCharacter.reduceEdit(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.NumberStepped ->
            copy(draft = draft.copy(level = draft.level.stepped(event.delta, PartyStatRanges.level)))
        is OverlayEvent.TextChanged ->
            copy(
                draft =
                    when (event.field) {
                        OverlayField.NAME -> draft.copy(name = event.value)
                        OverlayField.CLASS -> draft.copy(characterClass = event.value)
                        OverlayField.SUBCLASS -> draft.copy(subclass = event.value)
                        OverlayField.RACE -> draft.copy(race = event.value)
                        OverlayField.BACKGROUND -> draft.copy(background = event.value)
                        OverlayField.ALIGNMENT -> draft.copy(alignment = event.value)
                        OverlayField.PLAYER_NAME -> draft.copy(playerName = event.value)
                        OverlayField.ARMOR_CLASS_FROM, OverlayField.RESOURCE_NAME -> draft
                    },
            )
        else -> this
    }

internal fun FillDraft.reduce(event: OverlayEvent): FillDraft =
    when (event) {
        OverlayEvent.FillNext -> copy(step = (step + 1).coerceAtMost(FillDraft.LAST_STEP))
        OverlayEvent.FillSkip -> copy(step = (step + 1).coerceAtMost(FillDraft.LAST_STEP), skipped = skipped + step)
        OverlayEvent.FillBack -> copy(step = (step - 1).coerceAtLeast(FillDraft.STEP_CLASS))
        else -> reduceValues(event)
    }

private fun FillDraft.reduceValues(event: OverlayEvent): FillDraft =
    when (event) {
        is OverlayEvent.NumberStepped -> copy(level = level.stepped(event.delta, PartyStatRanges.level))
        is OverlayEvent.TextChanged ->
            when (event.field) {
                OverlayField.CLASS -> copy(characterClass = event.value)
                OverlayField.SUBCLASS -> copy(subclass = event.value)
                else -> this
            }
        is OverlayEvent.FillAbilityStepped -> {
            val score = (abilities[event.ability] ?: DEFAULT_SCORE).stepped(event.delta, SheetRanges.ability)
            copy(abilities = abilities + (event.ability to score))
        }
        is OverlayEvent.FillSaveToggled ->
            copy(saves = saves.toggled(event.ability))
        is OverlayEvent.FillSkillTapped ->
            copy(marks = marks + (event.skill to (marks[event.skill] ?: Proficiency.NONE).next()))
        else -> reduceVitals(event)
    }

private fun FillDraft.reduceVitals(event: OverlayEvent): FillDraft =
    when (event) {
        is OverlayEvent.FillHpMaxChanged -> copy(hpMax = event.value)
        is OverlayEvent.FillArmorClassStepped ->
            copy(armorClass = armorClass.stepped(event.delta, SheetRanges.armorClass))
        is OverlayEvent.FillSpeedStepped -> copy(speed = speed.stepped(event.delta, SheetRanges.speed))
        else -> this
    }

private fun Int.stepped(
    delta: Int,
    range: IntRange,
): Int = (this + delta).coerceIn(range)

internal fun Set<Ability>.toggled(ability: Ability): Set<Ability> =
    if (ability in
        this
    ) {
        this - ability
    } else {
        this + ability
    }
