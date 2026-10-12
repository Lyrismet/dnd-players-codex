package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.clampedToRanges

private fun QuickEditValue.text(): String? = (this as? QuickEditValue.Text)?.text

private fun QuickEditValue.number(): QuickEditValue.Number? = this as? QuickEditValue.Number

private fun QuickEditValue.places(): QuickEditValue.Places? = this as? QuickEditValue.Places

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
        QuickEditField.PLACE -> value.places()?.let { copy(locationIds = it.ids) }
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
        QuickEditField.PLACE -> value.places()?.let { copy(locationId = it.ids.firstOrNull()) }
        else -> null
    }

fun Location.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): Location? =
    when (field) {
        QuickEditField.NAME -> value.text()?.let { copy(name = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(description = it) }
        QuickEditField.PARENT -> value.places()?.let { copy(parentId = it.ids.firstOrNull()) }
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
        QuickEditField.LEVEL -> value.number()?.let { copy(level = it.number) }
        QuickEditField.HP_MAX -> value.number()?.let { copy(hpMax = it.number) }
        else -> null
    }
