package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppNameInput
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.feature.campaign.presentation.sheet.savableCampaignName
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_rename_hint
import dndplayerscodex.shared.generated.resources.settings_rename_placeholder
import dndplayerscodex.shared.generated.resources.settings_rename_save
import dndplayerscodex.shared.generated.resources.settings_rename_title
import dndplayerscodex.shared.generated.resources.settings_section_campaign
import org.jetbrains.compose.resources.stringResource

/** the campaign rename sheet - save is disabled and refused while the draft is blank */
@Composable
fun CampaignRenameSheet(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSave = savableCampaignName(draft) != null
    AppBottomSheet(onDismissRequest = onDismiss) {
        AppSheetColumn(spacing = 14.dp, scroll = true, header = {
            AppSheetHeader(
                overline = stringResource(Res.string.settings_section_campaign),
                title = stringResource(Res.string.settings_rename_title),
                onClose = onDismiss,
            )
        }) {
            AppNameInput(
                name = draft,
                onNameChanged = onDraftChanged,
                placeholder = stringResource(Res.string.settings_rename_placeholder),
                onDone = { if (canSave) onSave() },
            )
            Text(
                text = stringResource(Res.string.settings_rename_hint),
                fontSize = AppFontSize.SmallPlus,
                color = AppPalette.TextSecondary,
            )
            FormPrimaryButton(
                text = stringResource(Res.string.settings_rename_save),
                onClick = onSave,
                enabled = canSave,
            )
        }
    }
}
