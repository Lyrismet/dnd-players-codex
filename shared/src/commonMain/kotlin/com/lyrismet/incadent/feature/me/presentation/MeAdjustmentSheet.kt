package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.feature.codex.domain.model.SheetAdjustment
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_ac_from_hint
import incadent.shared.generated.resources.me_adjustment_hint
import incadent.shared.generated.resources.me_adjustment_note_label
import incadent.shared.generated.resources.me_adjustment_note_placeholder
import incadent.shared.generated.resources.me_popover_adjustment
import incadent.shared.generated.resources.me_popover_initiative
import incadent.shared.generated.resources.me_popover_passive
import incadent.shared.generated.resources.me_spells_tile_attack
import incadent.shared.generated.resources.me_spells_tile_dc
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AdjustmentSheet(
    edit: MeOverlay.Adjustment,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_popover_adjustment),
        title = stringResource(edit.target.titleRes()),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.value, send, spread = true, signed = true)
        Text(stringResource(Res.string.me_adjustment_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
        AdjustmentNote(edit.note, send)
    }
}

private fun SheetAdjustment.titleRes(): StringResource =
    when (this) {
        SheetAdjustment.INITIATIVE -> Res.string.me_popover_initiative
        SheetAdjustment.PASSIVE_PERCEPTION -> Res.string.me_popover_passive
        SheetAdjustment.SPELL_DC -> Res.string.me_spells_tile_dc
        SheetAdjustment.SPELL_ATTACK -> Res.string.me_spells_tile_attack
    }

/** the note of an adjustment, for the player only */
@Composable
internal fun AdjustmentNote(
    note: String,
    send: (OverlayEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.me_adjustment_note_label),
        value = note,
        onChange = { send(OverlayEvent.TextChanged(OverlayField.ADJUSTMENT_NOTE, it)) },
        placeholder = stringResource(Res.string.me_adjustment_note_placeholder),
    )
    Text(stringResource(Res.string.me_ac_from_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
}
