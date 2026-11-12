package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.FORM_AREA_MIN_LINES
import com.lyrismet.incadent.core.designsystem.component.FormFieldAction
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormNumberField
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.formAreaInputStyle
import com.lyrismet.incadent.feature.mention.presentation.MentionField
import com.lyrismet.incadent.feature.mention.presentation.MentionTextField
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.codex_entry_label_nicknames
import incadent.shared.generated.resources.codex_entry_placeholder_nicknames
import org.jetbrains.compose.resources.stringResource

/** a one-line form field that reports its text as the [field] of the entry */
@Composable
internal fun CodexEntryTextField(
    label: String,
    value: String,
    field: CodexEntryField,
    eventSink: (CodexEntryFormEvent) -> Unit,
    placeholder: String? = null,
    required: Boolean = false,
    maxLength: Int = TextLimits.LINE,
) {
    FormTextField(
        label = label,
        value = value,
        onChange = { eventSink(CodexEntryFormEvent.EntryFieldChanged(field, it)) },
        placeholder = placeholder,
        required = required,
        maxLength = maxLength,
    )
}

/** a multi-line form field that reports its text as the [field] of the entry, with "@" when [mention] is set */
@Composable
internal fun CodexEntryTextArea(
    label: String,
    value: String,
    field: CodexEntryField,
    eventSink: (CodexEntryFormEvent) -> Unit,
    placeholder: String? = null,
    mention: MentionField? = null,
) {
    if (mention == null) {
        FormTextArea(
            label = label,
            value = value,
            onChange = { eventSink(CodexEntryFormEvent.EntryFieldChanged(field, it)) },
            placeholder = placeholder,
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(label)
        MentionTextField(
            text = value,
            onTextChange = { eventSink(CodexEntryFormEvent.EntryFieldChanged(field, it)) },
            source = mention.source,
            self = mention.self,
            placeholder = placeholder,
            style = formAreaInputStyle(),
            minLines = FORM_AREA_MIN_LINES,
        )
    }
}

/** a number form field that reports its value as the [field] of the entry */
@Composable
internal fun CodexEntryNumberField(
    label: String,
    value: Int?,
    range: IntRange,
    field: CodexEntryNumberField,
    eventSink: (CodexEntryFormEvent) -> Unit,
    action: FormFieldAction? = null,
) {
    FormNumberField(
        label = label,
        value = value,
        range = range,
        onChange = { eventSink(CodexEntryFormEvent.EntryNumberChanged(field, it)) },
        action = action,
    )
}

/** the comma separated nicknames of an npc */
@Composable
internal fun NicknamesField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_nicknames),
        value = form.nicknames,
        field = CodexEntryField.NICKNAMES,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_nicknames),
        maxLength = TextLimits.NICKNAMES,
    )
}
