package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_edit_alignment_label
import dndplayerscodex.shared.generated.resources.me_edit_background_label
import dndplayerscodex.shared.generated.resources.me_edit_name_label
import dndplayerscodex.shared.generated.resources.me_edit_overline
import dndplayerscodex.shared.generated.resources.me_edit_player_label
import dndplayerscodex.shared.generated.resources.me_edit_race_label
import dndplayerscodex.shared.generated.resources.me_edit_title
import dndplayerscodex.shared.generated.resources.me_fill_class_label
import dndplayerscodex.shared.generated.resources.me_fill_level_label
import dndplayerscodex.shared.generated.resources.me_fill_subclass_label
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
        EditField(Res.string.me_fill_class_label, draft.characterClass, OverlayField.CLASS, send)
        EditField(Res.string.me_fill_subclass_label, draft.subclass, OverlayField.SUBCLASS, send)
        SectionOverline(stringResource(Res.string.me_fill_level_label), color = AppPalette.TextTertiary)
        NumberRow(draft.level, send)
        EditField(Res.string.me_edit_race_label, draft.race, OverlayField.RACE, send)
        EditField(Res.string.me_edit_background_label, draft.background, OverlayField.BACKGROUND, send)
        EditField(Res.string.me_edit_alignment_label, draft.alignment, OverlayField.ALIGNMENT, send)
        EditField(Res.string.me_edit_player_label, draft.playerName, OverlayField.PLAYER_NAME, send)
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
