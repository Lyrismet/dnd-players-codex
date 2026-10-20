package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_delete_button
import org.jetbrains.compose.resources.stringResource

/** the card's last block, shown only when the actions carry a delete handler */
@Composable
internal fun EntityDeleteButton(
    ref: EntityRef,
    actions: EntitySummarySheetActions,
) {
    val onDelete = actions.onDeleteClicked ?: return
    Spacer(Modifier.height(24.dp))
    AppButton(
        text = stringResource(Res.string.entity_sheet_delete_button),
        onClick = { onDelete(ref) },
        modifier = Modifier.fillMaxWidth(),
        tone = ButtonTone.Maroon,
        variant = ButtonVariant.Ghost,
        size = ButtonSize.Large,
    )
}
