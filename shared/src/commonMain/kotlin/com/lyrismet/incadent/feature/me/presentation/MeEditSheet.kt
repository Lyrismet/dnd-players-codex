package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.grouping.classEntriesOf
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.presentation.form.ClassListBlock
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.class_list_add
import incadent.shared.generated.resources.me_edit_alignment_label
import incadent.shared.generated.resources.me_edit_background_label
import incadent.shared.generated.resources.me_edit_bonus_catch_up
import incadent.shared.generated.resources.me_edit_bonus_hint
import incadent.shared.generated.resources.me_edit_bonus_label
import incadent.shared.generated.resources.me_edit_name_label
import incadent.shared.generated.resources.me_edit_overline
import incadent.shared.generated.resources.me_edit_race_label
import incadent.shared.generated.resources.me_edit_title
import incadent.shared.generated.resources.me_fill_class_label
import incadent.shared.generated.resources.me_fill_class_placeholder
import incadent.shared.generated.resources.me_fill_level_label
import incadent.shared.generated.resources.me_fill_subclass_label
import incadent.shared.generated.resources.me_fill_subclass_placeholder
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** the character's own fields, name and class to how they are signed */
@Composable
internal fun MeEditSheet(
    draft: CharacterDraft,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_edit_overline),
        title = stringResource(Res.string.me_edit_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        EditField(Res.string.me_edit_name_label, draft.name, OverlayField.NAME, send)
        MeClassFields(draft.characterClass, draft.subclass, draft.level, draft.classes, send)
        EditField(Res.string.me_edit_race_label, draft.race, OverlayField.RACE, send)
        EditField(Res.string.me_edit_background_label, draft.background, OverlayField.BACKGROUND, send)
        EditField(Res.string.me_edit_alignment_label, draft.alignment, OverlayField.ALIGNMENT, send)
        SectionOverline(stringResource(Res.string.me_edit_bonus_label), color = AppPalette.TextTertiary)
        NumberRow(draft.hpBonusPerLevel, send, onStep = { OverlayEvent.BonusStepped(it) })
        Text(stringResource(Res.string.me_edit_bonus_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
        draft.catchUp?.let {
            SwitchRow(stringResource(Res.string.me_edit_bonus_catch_up, it.signed()), draft.applyCatchUp) {
                send(OverlayEvent.CatchUpToggled)
            }
        }
    }
}

@Composable
private fun EditField(
    label: StringResource,
    value: String,
    field: OverlayField,
    send: (OverlayEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(label),
        value = value,
        onChange = { send(OverlayEvent.TextChanged(field, it)) },
    )
}

/** the class, subclass and level as plain fields, or as class rows once the character has several classes */
@Composable
internal fun MeClassFields(
    characterClass: String,
    subclass: String,
    level: Int,
    classes: List<PartyClass>,
    send: (OverlayEvent) -> Unit,
    placeholders: Boolean = false,
) {
    val multi = classes.size >= 2
    if (multi) {
        FormFieldLabel(stringResource(Res.string.me_fill_class_label))
    } else {
        FormTextField(
            label = stringResource(Res.string.me_fill_class_label),
            value = characterClass,
            onChange = { send(OverlayEvent.TextChanged(OverlayField.CLASS, it)) },
            placeholder = if (placeholders) stringResource(Res.string.me_fill_class_placeholder) else "",
        )
        FormTextField(
            label = stringResource(Res.string.me_fill_subclass_label),
            value = subclass,
            onChange = { send(OverlayEvent.TextChanged(OverlayField.SUBCLASS, it)) },
            placeholder = if (placeholders) stringResource(Res.string.me_fill_subclass_placeholder) else "",
        )
        SectionOverline(stringResource(Res.string.me_fill_level_label), color = AppPalette.TextTertiary)
        NumberRow(level, send)
    }
    ClassListBlock(
        entries =
            remember(classes, characterClass, subclass, level) {
                classEntriesOf(classes, characterClass, subclass, level)
            },
        addLabel = stringResource(Res.string.class_list_add),
        onChange = { send(OverlayEvent.ClassesChanged(it)) },
    )
}
