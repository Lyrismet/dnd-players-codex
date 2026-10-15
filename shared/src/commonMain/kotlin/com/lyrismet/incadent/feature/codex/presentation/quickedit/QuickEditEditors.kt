package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputCounter
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.PillFill
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.summary.RelationOption
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.quick_edit_description_hint
import dndplayerscodex.shared.generated.resources.quick_edit_done
import dndplayerscodex.shared.generated.resources.quick_edit_link_none
import org.jetbrains.compose.resources.stringResource

/** the editor frame - gold ring, 12dp corners, 8/10/10 padding and an 8dp gap, from the mockup's edit block */
@Composable
private fun EditorFrame(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val tracker = LocalEditorHoleTracker.current
    DisposableEffect(tracker) { onDispose { tracker?.clear() } }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { tracker?.report(it) }
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.Gold,
                    borderWidth = 1.5.dp,
                ).padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/** the editor that replaces a field's value - a label row with Отмена / Готово above a bare input */
@Composable
fun InlineFieldEditor(
    label: String,
    draft: String,
    kind: InlineInputKind,
    onEvent: (QuickEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(draft, editorCaretAtEnd(draft))) }
    val maxLength = if (kind == InlineInputKind.MULTILINE) TextLimits.NOTE else TextLimits.LINE
    val isArea = kind == InlineInputKind.MULTILINE
    EditorFrame(modifier) {
        EditorHeader(label) {
            AppButton(
                text = stringResource(Res.string.quick_edit_done),
                onClick = { onEvent(QuickEditUiEvent.EditSaved) },
                variant = ButtonVariant.Solid,
            )
        }
        AppTextInput(
            value = value,
            onValueChange = {
                value = it
                onEvent(QuickEditUiEvent.DraftChanged(it.text))
            },
            placeholder = if (isArea) stringResource(Res.string.quick_edit_description_hint) else null,
            style =
                AppTextInputStyle(
                    textStyle = inputTextStyle(kind),
                    chrome = null,
                    contentPadding = PaddingValues(),
                    placeholderColor = AppPalette.TextTertiary,
                ),
            singleLine = !isArea,
            minLines = if (isArea) 5 else 1,
            maxLength = maxLength,
            counter = AppTextInputCounter(maxLength),
            keyboardOptions = inputKeyboardOptions(kind),
            keyboardActions = KeyboardActions(onDone = { onEvent(QuickEditUiEvent.EditSaved) }),
            autoFocus = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(inputHeight(kind))
                    .onPreviewKeyEvent { event ->
                        val commandPressed = event.isCtrlPressed || event.isMetaPressed
                        handleEditorKey(event.type, event.key, commandPressed, onEvent)
                    },
        )
    }
}

/** re-points a link fact - a header with Отмена only, then "Не указано" plus one button per candidate */
@Composable
fun LinkOptionEditor(
    label: String,
    options: List<MentionChipItem>,
    selectedId: Long?,
    onEvent: (QuickEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EditorFrame(modifier) {
        EditorHeader(label) {}
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SelectablePill(
                text = stringResource(Res.string.quick_edit_link_none),
                selected = selectedId == null,
                onClick = { onEvent(QuickEditUiEvent.LinkChosen(null)) },
                selectedFill = PillFill.Neutral,
            )
            options.forEach { option ->
                val id = option.entityRef?.id ?: return@forEach
                val selected = id == selectedId
                SelectablePill(
                    text = if (selected) "✓ ${option.label}" else option.label,
                    selected = selected,
                    onClick = { onEvent(QuickEditUiEvent.LinkChosen(id)) },
                    selectedFill = PillFill.Neutral,
                    selectedColor = option.color,
                )
            }
        }
    }
}

/** edits a relation group - one button per candidate, a tap links or unlinks it and the editor stays open */
@Composable
fun RelationOptionEditor(
    label: String,
    options: List<RelationOption>,
    hint: String,
    onEvent: (QuickEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EditorFrame(modifier) {
        EditorHeader(label) {}
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val id = option.chip.entityRef?.id ?: return@forEach
                SelectablePill(
                    text = if (option.isSelected) "✓ ${option.chip.label}" else option.chip.label,
                    selected = option.isSelected,
                    onClick = { onEvent(QuickEditUiEvent.RelationToggled(id)) },
                    selectedFill = PillFill.Neutral,
                    selectedColor = option.chip.color,
                )
            }
        }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextTertiary)
    }
}

@Composable
private fun EditorHeader(
    label: String,
    actions: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label.uppercase(),
            style = AppText.fieldOverline,
            color = AppPalette.GoldBright,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

private fun inputHeight(kind: InlineInputKind): Modifier =
    when (kind) {
        InlineInputKind.TITLE -> Modifier.height(38.dp)
        InlineInputKind.TEXT, InlineInputKind.NUMBER -> Modifier.height(36.dp)
        InlineInputKind.MULTILINE -> Modifier
    }

// mockup - title is serif 24/600, fields are Inter 15px, description is Inter 15px with a 1.6 line-height
@Composable
private fun inputTextStyle(kind: InlineInputKind) =
    when (kind) {
        InlineInputKind.TITLE ->
            MaterialTheme.typography.headlineLarge.copy(
                fontSize = AppFontSize.SubDisplay,
                lineHeight = 29.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.TextHeading,
            )
        InlineInputKind.TEXT, InlineInputKind.NUMBER ->
            MaterialTheme.typography.bodyLarge.copy(fontSize = AppFontSize.Large, color = AppPalette.TextPrimary)
        InlineInputKind.MULTILINE ->
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = AppFontSize.Large,
                lineHeight = 24.sp,
                color = AppPalette.TextPrimary,
            )
    }

private fun inputKeyboardOptions(kind: InlineInputKind): KeyboardOptions =
    when (kind) {
        InlineInputKind.NUMBER -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        InlineInputKind.MULTILINE ->
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default)
        InlineInputKind.TITLE, InlineInputKind.TEXT ->
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
    }

private fun handleEditorKey(
    type: KeyEventType,
    key: Key,
    commandPressed: Boolean,
    onEvent: (QuickEditUiEvent) -> Unit,
): Boolean {
    val event =
        when {
            type != KeyEventType.KeyDown -> null
            key == Key.Escape -> QuickEditUiEvent.EditCancelled
            key == Key.Enter && commandPressed -> QuickEditUiEvent.EditSaved
            else -> null
        }
    event?.let(onEvent)
    return event != null
}
