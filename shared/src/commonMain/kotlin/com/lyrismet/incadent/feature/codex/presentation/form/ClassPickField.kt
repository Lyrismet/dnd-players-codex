package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_class
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_class
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
