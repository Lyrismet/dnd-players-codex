package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormChipQuickAdd
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_helper
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_location
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_npc
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_placeholder_location
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_placeholder_npc
import org.jetbrains.compose.resources.stringResource

/** the "+ New place" quick-add for a location-type chip field - [field] is which of the two owns the open input */
@Composable
internal fun locationQuickAdd(
    form: CodexEntryFormState,
    eventSink: (CodexEvent) -> Unit,
    field: CodexEntryChipField,
): FormChipQuickAdd =
    quickAddFor(
        form = form,
        eventSink = eventSink,
        field = field,
        addLabel = stringResource(Res.string.codex_entry_quick_add_location),
        placeholder = stringResource(Res.string.codex_entry_quick_add_placeholder_location),
    )

/** the "+ New NPC" quick-add for the quest "given by" chip field */
@Composable
internal fun npcQuickAdd(
    form: CodexEntryFormState,
    eventSink: (CodexEvent) -> Unit,
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
    eventSink: (CodexEvent) -> Unit,
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
        onOpen = { eventSink(CodexEvent.EntryQuickAddOpened(field)) },
        onDraftChanged = { eventSink(CodexEvent.EntryQuickAddDraftChanged(it)) },
        onSave = { eventSink(CodexEvent.EntryQuickAddSaveClicked) },
        onCancel = { eventSink(CodexEvent.EntryQuickAddCancelled) },
    )
}
