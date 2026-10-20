package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormFieldAction
import com.lyrismet.incadent.core.designsystem.component.FormNumberField
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_nicknames
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_nicknames
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

/** a multi-line form field that reports its text as the [field] of the entry */
@Composable
internal fun CodexEntryTextArea(
    label: String,
    value: String,
    field: CodexEntryField,
    eventSink: (CodexEntryFormEvent) -> Unit,
    placeholder: String? = null,
) {
    FormTextArea(
        label = label,
        value = value,
        onChange = { eventSink(CodexEntryFormEvent.EntryFieldChanged(field, it)) },
        placeholder = placeholder,
    )
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

/** the comma separated nicknames every record type can carry */
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
