package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormChipQuickAdd
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_helper
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_npc
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_placeholder_npc
import org.jetbrains.compose.resources.stringResource

/** the "+ New NPC" quick-add for the quest "given by" chip field */
@Composable
internal fun npcQuickAdd(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    field: CodexEntryChipField,
): FormChipQuickAdd =
    quickAddFor(
        form = form,
        eventSink = eventSink,
        field = field,
        addLabel = stringResource(Res.string.codex_entry_quick_add_npc),
        placeholder = stringResource(Res.string.codex_entry_quick_add_placeholder_npc),
    )

@Composable
internal fun quickAddFor(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    field: CodexEntryChipField,
    addLabel: String,
    placeholder: String,
): FormChipQuickAdd {
    val isOpen = form.quickAddField == field
    return FormChipQuickAdd(
        addLabel = addLabel,
        isOpen = isOpen,
        draft = if (isOpen) form.quickAddDraft else "",
        placeholder = placeholder,
        helperText = stringResource(Res.string.codex_entry_quick_add_helper),
        onOpen = { eventSink(CodexEntryFormEvent.EntryQuickAddOpened(field)) },
        onDraftChanged = { eventSink(CodexEntryFormEvent.EntryQuickAddDraftChanged(it)) },
        onSave = { eventSink(CodexEntryFormEvent.EntryQuickAddSaveClicked) },
        onCancel = { eventSink(CodexEntryFormEvent.EntryQuickAddCancelled) },
    )
}
