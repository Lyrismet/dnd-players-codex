package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormFieldAction
import com.lyrismet.incadent.core.designsystem.component.FormNumberField
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField

/** a one-line form field that reports its text as the [field] of the entry */
@Composable
internal fun CodexEntryTextField(
    label: String,
    value: String,
    field: CodexEntryField,
    eventSink: (CodexEntryFormEvent) -> Unit,
    placeholder: String? = null,
    required: Boolean = false,
) {
    FormTextField(
        label = label,
        value = value,
        onChange = { eventSink(CodexEntryFormEvent.EntryFieldChanged(field, it)) },
        placeholder = placeholder,
        required = required,
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
