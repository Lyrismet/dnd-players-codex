package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.campaign.savableCampaignName
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionOverline(
                        text = stringResource(Res.string.settings_section_campaign),
                        color = AppPalette.Gold,
                        letterSpacing = 0.16.em,
                    )
                    Text(
                        text = stringResource(Res.string.settings_rename_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 31.sp),
                        color = AppPalette.TextHeading,
                    )
                }
                SheetCloseButton(onClick = onDismiss, size = 40.dp)
            }
            RenameNameInput(draft = draft, onDraftChanged = onDraftChanged, onDone = { if (canSave) onSave() })
            Text(
                text = stringResource(Res.string.settings_rename_hint),
                fontSize = 12.5.sp,
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
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .appCard(
                    shape = RoundedCornerShape(12.dp),
                    background = AppPalette.Background,
                    border = AppPalette.Gold.copy(alpha = 0.5f),
                ).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle =
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.TextHeading,
                ),
            cursorBrush = GoldCursorBrush,
            keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { innerField ->
                if (draft.isEmpty()) {
                    Text(
                        stringResource(Res.string.settings_rename_placeholder),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                        color = AppPalette.TextTertiary,
                    )
                }
                innerField()
            },
        )
    }
}
