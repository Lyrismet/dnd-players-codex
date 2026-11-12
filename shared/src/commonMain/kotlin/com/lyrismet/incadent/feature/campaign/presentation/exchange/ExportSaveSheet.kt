package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppListCard
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.file.rememberFileSaver
import com.lyrismet.incadent.feature.campaign.domain.exchange.MAX_TYPED_CHARS
import com.lyrismet.incadent.feature.campaign.domain.exchange.typedFileName
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.exchange_save_cancel
import incadent.shared.generated.resources.exchange_save_download
import incadent.shared.generated.resources.exchange_save_folder
import incadent.shared.generated.resources.exchange_save_folder_ask
import incadent.shared.generated.resources.exchange_save_folder_downloads
import incadent.shared.generated.resources.exchange_save_name
import incadent.shared.generated.resources.exchange_save_title
import org.jetbrains.compose.resources.stringResource

private const val CANCEL_WEIGHT = 1f
private const val DOWNLOAD_WEIGHT = 1.5f

/** asks for the file name and the folder of a saved export, and weighs the file on the button that writes it */
@Composable
fun ExportSaveSheet(
    prompt: SavePrompt,
    onEvent: (ExportEvent) -> Unit,
) {
    val saver =
        rememberFileSaver(
            onFolder = { onEvent(ExportEvent.SaveFolderPicked(it)) },
            onSaved = { onEvent(ExportEvent.Delivered(saved = it)) },
        )
    val target = typedFileName(prompt.name)
    val folder =
        prompt.folder?.name
            ?: stringResource(
                if (saver.hasDefaultFolder) {
                    Res.string.exchange_save_folder_downloads
                } else {
                    Res.string.exchange_save_folder_ask
                },
            )
    val cancel = { onEvent(ExportEvent.SaveCancelled) }
    AppBottomSheet(onDismissRequest = cancel) {
        AppSheetColumn(spacing = 14.dp, header = {
            AppSheetHeader(title = stringResource(Res.string.exchange_save_title), onClose = cancel)
        }) {
            Text(
                text = stringResource(Res.string.exchange_save_name),
                color = AppPalette.TextSecondary,
                style = AppText.footnote,
            )
            AppTextInput(
                value = prompt.name,
                onValueChange = { onEvent(ExportEvent.SaveNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                maxLength = MAX_TYPED_CHARS,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            )
            AppListCard {
                AppListRow(
                    title = stringResource(Res.string.exchange_save_folder),
                    value = folder,
                    valueColor = AppPalette.TextPrimary,
                    onClick = saver.pickFolder,
                )
            }
            SaveButtons(
                size = fileSizeText(prompt.sizeBytes),
                canSave = target.isNotEmpty(),
                onCancel = cancel,
                onSave = { saver.save(prompt.source, target, prompt.folder) },
            )
        }
    }
}

@Composable
private fun SaveButtons(
    size: String,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppButton(
            text = stringResource(Res.string.exchange_save_cancel),
            onClick = onCancel,
            modifier = Modifier.weight(CANCEL_WEIGHT),
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Primary,
            contentColor = AppPalette.TextPrimary,
        )
        AppButton(
            text = stringResource(Res.string.exchange_save_download, size),
            onClick = onSave,
            modifier = Modifier.weight(DOWNLOAD_WEIGHT),
            variant = ButtonVariant.Solid,
            size = ButtonSize.Primary,
            enabled = canSave,
        )
    }
}
