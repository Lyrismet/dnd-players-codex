package com.lyrismet.incadent.core.quickedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
import com.lyrismet.incadent.core.designsystem.component.appCard
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
                    shape = RoundedCornerShape(12.dp),
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
    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(draft, editorCaretAtEnd(draft))) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    EditorFrame(modifier) {
        EditorHeader(label) {
            HeaderActionButton(
                text = stringResource(Res.string.quick_edit_done),
                onClick = { onEvent(QuickEditUiEvent.EditSaved) },
                foreground = AppPalette.Background,
                background = AppPalette.Gold,
                border = AppPalette.Gold,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                onEvent(QuickEditUiEvent.DraftChanged(it.text))
            },
            textStyle = inputTextStyle(kind),
            cursorBrush = GoldCursorBrush,
            singleLine = kind != InlineInputKind.MULTILINE,
            minLines = if (kind == InlineInputKind.MULTILINE) 5 else 1,
            keyboardOptions = inputKeyboardOptions(kind),
            keyboardActions = KeyboardActions(onDone = { onEvent(QuickEditUiEvent.EditSaved) }),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty() && kind == InlineInputKind.MULTILINE) {
                        Text(
                            stringResource(Res.string.quick_edit_description_hint),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp),
                            color = AppPalette.TextTertiary,
                        )
                    }
                    inner()
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(inputHeight(kind))
                    .focusRequester(focusRequester)
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
            LinkOptionButton(
                text = stringResource(Res.string.quick_edit_link_none),
                selected = selectedId == null,
                onClick = { onEvent(QuickEditUiEvent.LinkChosen(null)) },
            )
            options.forEach { option ->
                val id = option.entityRef?.id ?: return@forEach
                val selected = id == selectedId
                LinkOptionButton(
                    text = if (selected) "✓ ${option.label}" else option.label,
                    selected = selected,
                    onClick = { onEvent(QuickEditUiEvent.LinkChosen(id)) },
                    selectedColor = option.color,
                )
            }
        }
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
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.14.em),
            color = AppPalette.GoldBright,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

// mockup - min-height 38, radius 19, 13px/600 - the selected one takes the entity's colours, "Не указано" a neutral set
@Composable
private fun LinkOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: StatusColor? = null,
) {
    val foreground =
        when {
            !selected -> AppPalette.TextDescription
            selectedColor != null -> selectedColor.foreground
            else -> AppPalette.TextMuted
        }
    val background =
        when {
            !selected -> Color.Transparent
            selectedColor != null -> selectedColor.background
            else -> AppPalette.SurfacePopover
        }
    val border =
        when {
            !selected -> AppPalette.Border
            selectedColor != null -> selectedColor.border
            else -> AppPalette.BorderHover
        }
    Box(
        modifier =
            Modifier
                .appCard(shape = RoundedCornerShape(19.dp), background = background, border = border, onClick = onClick)
                .heightIn(min = 38.dp)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = foreground,
            fontWeight = FontWeight.SemiBold,
        )
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
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.TextHeading,
            )
        InlineInputKind.TEXT, InlineInputKind.NUMBER ->
            MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = AppPalette.TextPrimary)
        InlineInputKind.MULTILINE ->
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
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
