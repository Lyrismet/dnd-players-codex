package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.clampedToRanges

private fun QuickEditValue.text(): String? = (this as? QuickEditValue.Text)?.text

private fun QuickEditValue.number(): Int? = (this as? QuickEditValue.Number)?.number

private fun QuickEditValue.portrait(): String? = (this as? QuickEditValue.Portrait)?.base64

private fun QuickEditValue.link(): QuickEditValue.Link? = this as? QuickEditValue.Link

// the functions below return null for a field this entity does not have, so a wrong field never writes anything

fun Npc.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): Npc? =
    when (field) {
        QuickEditField.NAME -> value.text()?.let { copy(name = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(description = it) }
        QuickEditField.RACE -> value.text()?.let { copy(race = it) }
        QuickEditField.FACTION -> value.text()?.let { copy(faction = it) }
        QuickEditField.PLACE -> value.link()?.let { copy(locationId = it.id) }
        QuickEditField.PORTRAIT -> value.portrait()?.let { copy(portraitBase64 = it) }
        else -> null
    }

fun Quest.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): Quest? =
    when (field) {
        QuickEditField.NAME -> value.text()?.let { copy(title = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(description = it) }
        QuickEditField.REWARD -> value.text()?.let { copy(reward = it) }
        QuickEditField.GIVER -> value.link()?.let { copy(givenByNpcId = it.id) }
        QuickEditField.PLACE -> value.link()?.let { copy(locationId = it.id) }
        else -> null
    }

fun Location.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): Location? =
    when (field) {
        QuickEditField.NAME -> value.text()?.let { copy(name = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(description = it) }
        QuickEditField.LOCATION_TYPE -> value.text()?.let { copy(type = it) }
        QuickEditField.LOCATION_REGION -> value.text()?.let { copy(region = it) }
        else -> null
    }

/** stats are clamped after the write, so a lowered max hp also pulls the current hp down */
fun PartyMember.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): PartyMember? = (withTextQuickEdit(field, value) ?: withNumberQuickEdit(field, value))?.clampedToRanges()

private fun PartyMember.withTextQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): PartyMember? =
    when (field) {
        QuickEditField.NAME -> value.text()?.let { copy(name = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(description = it) }
        QuickEditField.RACE -> value.text()?.let { copy(race = it) }
        QuickEditField.CHARACTER_CLASS -> value.text()?.let { copy(characterClass = it) }
        QuickEditField.PLAYER_NAME -> value.text()?.let { copy(playerName = it) }
        QuickEditField.PORTRAIT -> value.portrait()?.let { copy(portraitBase64 = it) }
        else -> null
    }

private fun PartyMember.withNumberQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): PartyMember? =
    when (field) {
        QuickEditField.LEVEL -> value.number()?.let { copy(level = it) }
        QuickEditField.ARMOR_CLASS -> value.number()?.let { copy(armorClass = it) }
        QuickEditField.HP_MAX -> value.number()?.let { copy(hpMax = it) }
        QuickEditField.INITIATIVE_BONUS -> value.number()?.let { copy(initiativeBonus = it) }
        else -> null
    }
