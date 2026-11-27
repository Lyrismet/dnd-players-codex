package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.class_list_add_multi
import incadent.shared.generated.resources.codex_entry_label_class
import incadent.shared.generated.resources.codex_entry_placeholder_party_class
import org.jetbrains.compose.resources.stringResource

/** the class and subclass of a party member or an npc as one field, a press opens the class sheet */
@Composable
internal fun ClassPickField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_class),
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_class),
        onClick = { eventSink(CodexEntryFormEvent.EntryClassSelectOpened) },
        title = form.characterClassLine.takeIf { it.isNotBlank() },
    )
}

// one class is the plain field with the dashed tile under it, two or more are rows, each with its level
@Composable
internal fun PartyClassFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    val multi = form.partyClasses.size >= 2
    if (multi) {
        FormFieldLabel(stringResource(Res.string.codex_entry_label_class))
    } else {
        ClassPickField(form, eventSink)
    }
    ClassListBlock(
        entries = form.partyClasses,
        addLabel = stringResource(Res.string.class_list_add_multi),
        onChange = { eventSink(CodexEntryFormEvent.EntryClassesChanged(it)) },
    )
}
