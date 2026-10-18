package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppInputChrome
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputCounter
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.TextLimits
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
        AppSheetColumn(spacing = 14.dp) {
            AppSheetHeader(
                overline = stringResource(Res.string.settings_section_campaign),
                title = stringResource(Res.string.settings_rename_title),
                onClose = onDismiss,
            )
            RenameNameInput(
                draft = draft,
                onDraftChanged = { onDraftChanged(it.take(TextLimits.LINE)) },
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

@Composable
private fun RenameNameInput(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onDone: () -> Unit,
) {
    // the caret starts after the last letter so a long name scrolls to its end
    var value by remember { mutableStateOf(TextFieldValue(draft, TextRange(draft.length))) }
    AppTextInput(
        value = value,
        onValueChange = {
            value = it
            onDraftChanged(it.text)
        },
        autoFocus = true,
        placeholder = stringResource(Res.string.settings_rename_placeholder),
        style =
            AppTextInputStyle(
                textStyle =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = AppFontSize.Section,
                        fontWeight = FontWeight.SemiBold,
                    ),
                textColor = AppPalette.TextHeading,
                chrome = AppInputChrome(border = AppPalette.Gold.copy(alpha = 0.5f)),
                height = 56.dp,
            ),
        maxLength = TextLimits.LINE,
        counter = AppTextInputCounter(TextLimits.LINE),
        keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}
