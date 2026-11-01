package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.DiscardChangesDialog
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.format.kindLabelRes
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.feature.mention.presentation.MentionField
import com.lyrismet.incadent.feature.mention.presentation.MentionFieldState
import com.lyrismet.incadent.feature.mention.presentation.MentionTextField
import com.lyrismet.incadent.feature.mention.presentation.MentionTriggerButton
import com.lyrismet.incadent.feature.mention.presentation.rememberMentionFieldState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.notes_editor_at_action
import dndplayerscodex.shared.generated.resources.notes_editor_at_hint
import dndplayerscodex.shared.generated.resources.notes_editor_close
import dndplayerscodex.shared.generated.resources.notes_editor_discard_text
import dndplayerscodex.shared.generated.resources.notes_editor_overline_format
import dndplayerscodex.shared.generated.resources.notes_editor_placeholder
import dndplayerscodex.shared.generated.resources.notes_editor_save
import dndplayerscodex.shared.generated.resources.quick_edit_label_description
import org.jetbrains.compose.resources.stringResource

/** the description of a record over the whole screen, so the "@" list sits above the keyboard */
@Composable
internal fun NotesEditorScreen(
    draft: String,
    mention: MentionField,
    onEvent: (QuickEditUiEvent) -> Unit,
) {
    // a window floats above everything, so it goes with its tab like the other dialogs
    if (!LocalTabSelected.current) return
    val original = remember { draft }
    var confirmingLeave by remember { mutableStateOf(false) }
    val dirty = draft != original
    val requestClose = { if (dirty) confirmingLeave = true else onEvent(QuickEditUiEvent.EditCancelled) }
    val owner = mention.source.entities.firstOrNull { it.ref == mention.self }
    val state = rememberMentionFieldState(draft)
    Dialog(
        onDismissRequest = requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AppPalette.Background)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding(),
        ) {
            NotesEditorHeader(
                overline =
                    stringResource(
                        Res.string.notes_editor_overline_format,
                        stringResource(Res.string.quick_edit_label_description).uppercase(),
                        owner?.let { stringResource(it.ref.type.kindLabelRes()).uppercase() }.orEmpty(),
                    ),
                title = owner?.name.orEmpty(),
                onClose = requestClose,
            )
            // the same card the session composer draws around its input and its action row
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .appCard(
                            shape = AppShapes.Card,
                            background = AppPalette.SurfaceInput,
                            border = AppPalette.Border,
                        ).padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NotesEditorField(draft, mention, state, Modifier.weight(1f), onEvent)
                NotesEditorBar(
                    canSave = dirty,
                    onAt = state::insertTrigger,
                    onSave = { onEvent(QuickEditUiEvent.EditSaved) },
                )
            }
        }
    }
    if (confirmingLeave) {
        DiscardChangesDialog(
            text = stringResource(Res.string.notes_editor_discard_text),
            onStay = { confirmingLeave = false },
            onLeave = { onEvent(QuickEditUiEvent.EditCancelled) },
        )
    }
}

@Composable
private fun NotesEditorField(
    draft: String,
    mention: MentionField,
    state: MentionFieldState,
    modifier: Modifier,
    onEvent: (QuickEditUiEvent) -> Unit,
) {
    MentionTextField(
        text = draft,
        onTextChange = { onEvent(QuickEditUiEvent.DraftChanged(it)) },
        source = mention.source,
        self = mention.self,
        state = state,
        modifier = modifier,
        placeholder = stringResource(Res.string.notes_editor_placeholder),
        style =
            AppTextInputStyle(
                textStyle = inputTextStyle(InlineInputKind.MULTILINE),
                chrome = null,
                contentPadding = PaddingValues(),
                placeholderColor = AppPalette.TextTertiary,
            ),
        maxLength = TextLimits.NOTE,
        fillHeight = true,
        autoFocus = true,
    )
}

@Composable
private fun NotesEditorHeader(
    overline: String,
    title: String,
    onClose: () -> Unit,
) {
    val closeLabel = stringResource(Res.string.notes_editor_close)
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        IconBadge(
            modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = closeLabel },
            size = 44.dp,
            background = Color.Transparent,
            onClick = onClose,
        ) {
            Icon(
                AppIcons.Close,
                contentDescription = null,
                tint = AppPalette.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(overline, style = AppText.fieldOverline, color = AppPalette.Gold)
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading, maxLines = 1)
        }
    }
}

@Composable
private fun NotesEditorBar(
    canSave: Boolean,
    onAt: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MentionTriggerButton(
            onClick = onAt,
            size = 40.dp,
            description = stringResource(Res.string.notes_editor_at_action),
        )
        Text(
            stringResource(Res.string.notes_editor_at_hint),
            style = AppText.caption,
            color = AppPalette.TextTertiary,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.notes_editor_save),
            onClick = onSave,
            enabled = canSave,
            variant = ButtonVariant.Solid,
            size = ButtonSize.Large,
        )
    }
}
