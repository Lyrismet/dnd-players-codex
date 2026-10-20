package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_move_into
import org.jetbrains.compose.resources.stringResource

/** "Переместить в…" for a place card, placed by the caller next to the delete button - only where editable in place */
@Composable
internal fun EntityPlaceMoveButton(
    location: EntitySummaryItem.LocationSummary,
    quick: QuickEditSheet?,
) {
    if (quick == null || !location.canMove) return
    AppButton(
        text = stringResource(Res.string.entity_sheet_move_into),
        onClick = { quick.onEvent(QuickEditUiEvent.FieldHeld(QuickEditField.MOVE, "")) },
        modifier = Modifier.fillMaxWidth(),
        tone = ButtonTone.Neutral,
        variant = ButtonVariant.Ghost,
        size = ButtonSize.Large,
    )
}
