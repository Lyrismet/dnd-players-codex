package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.feature.codex.domain.model.Species

fun Species.withQuickEdit(
    field: QuickEditField,
    value: QuickEditValue,
): Species? =
    when (field) {
        QuickEditField.NAME -> value.name?.let { copy(name = it) }
        QuickEditField.DESCRIPTION -> value.text()?.let { copy(notes = it) }
        QuickEditField.CREATURE_TYPE -> (value as? QuickEditValue.Creature)?.let { withType(it.type) }
        QuickEditField.HIT_POINTS -> (value as? QuickEditValue.Number)?.let { copy(hitPoints = it.number) }
        QuickEditField.PORTRAIT -> value.picture()?.let { copy(photo = it.photo) }
        else -> null
    }
