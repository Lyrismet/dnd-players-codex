package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormFieldAction
import com.lyrismet.incadent.core.designsystem.component.FormNumberField
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.designsystem.component.QuickAddOutsideTapHost
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.domain.model.PartyStatRanges
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_discard_back
import dndplayerscodex.shared.generated.resources.codex_entry_discard_leave
import dndplayerscodex.shared.generated.resources.codex_entry_discard_text
import dndplayerscodex.shared.generated.resources.codex_entry_discard_title
import dndplayerscodex.shared.generated.resources.codex_entry_label_class
import dndplayerscodex.shared.generated.resources.codex_entry_label_description
import dndplayerscodex.shared.generated.resources.codex_entry_label_faction
import dndplayerscodex.shared.generated.resources.codex_entry_label_giver
import dndplayerscodex.shared.generated.resources.codex_entry_label_hp_max
import dndplayerscodex.shared.generated.resources.codex_entry_label_level
import dndplayerscodex.shared.generated.resources.codex_entry_label_life
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_location
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_npc
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_party
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_quest
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
import dndplayerscodex.shared.generated.resources.codex_entry_label_owner
import dndplayerscodex.shared.generated.resources.codex_entry_label_parent
import dndplayerscodex.shared.generated.resources.codex_entry_label_player
import dndplayerscodex.shared.generated.resources.codex_entry_label_presence
import dndplayerscodex.shared.generated.resources.codex_entry_label_race
import dndplayerscodex.shared.generated.resources.codex_entry_label_relationship
import dndplayerscodex.shared.generated.resources.codex_entry_label_reward
import dndplayerscodex.shared.generated.resources.codex_entry_label_status
import dndplayerscodex.shared.generated.resources.codex_entry_label_where
import dndplayerscodex.shared.generated.resources.codex_entry_label_where_to_meet
import dndplayerscodex.shared.generated.resources.codex_entry_party_level_from_player
import dndplayerscodex.shared.generated.resources.codex_entry_pick_place_hint
import dndplayerscodex.shared.generated.resources.codex_entry_pick_places_hint
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_description
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_faction
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_notes
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_race
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_class
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_notes
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_player
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_party_race
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_quest_description
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_quest_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_quest_reward
import dndplayerscodex.shared.generated.resources.codex_entry_required_note
import dndplayerscodex.shared.generated.resources.codex_entry_type_location
import dndplayerscodex.shared.generated.resources.codex_entry_type_npc
import dndplayerscodex.shared.generated.resources.codex_entry_type_party
import dndplayerscodex.shared.generated.resources.codex_entry_type_quest
import org.jetbrains.compose.resources.stringResource

private val CloseButtonSize = 40.dp

/** codex create/edit sheet body - renders [form]'s type-specific fields and dispatches through [eventSink] */
@Composable
internal fun CodexEntryFormUi(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    QuickAddOutsideTapHost(
        enabled = form.quickAddField != null,
        onOutsideTap = {
            focusManager.clearFocus()
            eventSink(CodexEvent.EntryQuickAddCancelled)
        },
        modifier = modifier,
    ) {
        CodexEntryFormContent(form, eventSink, onClose)
    }
}

@Composable
private fun CodexEntryFormContent(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                // shrinks the scroll viewport above the keyboard, so the open quick-add panel can scroll clear of it
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                // matches every bottom-sheet body's content inset, see Players Codex v5.dc.html sheetRef 6px 20px 44px
                .padding(top = 6.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // overline and heading sit flush, only the blocks around them get the 16dp rhythm
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionOverline(text = form.overline, color = AppPalette.Gold)
                Text(form.heading, style = MaterialTheme.typography.headlineLarge, color = AppPalette.TextHeading)
            }
            SheetCloseButton(onClick = onClose, size = CloseButtonSize)
        }
        if (!form.isEditing) {
            CodexEntryTypeSwitch(selected = form.type, onSelected = { eventSink(CodexEvent.EntryTypeChanged(it)) })
        }
        when (form.type) {
            CodexEntryType.PARTY -> PartyEntryFields(form, eventSink)
            CodexEntryType.NPC -> NpcEntryFields(form, eventSink)
            CodexEntryType.QUEST -> QuestEntryFields(form, eventSink)
            CodexEntryType.LOCATION -> LocationEntryFields(form, eventSink)
        }
        Text(
            stringResource(Res.string.codex_entry_required_note),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.TextSecondary,
        )
        FormPrimaryButton(
            text = form.saveLabel,
            onClick = { eventSink(CodexEvent.EntryFormSaveClicked) },
            enabled = form.canSave,
        )
    }
}

@Composable
private fun NpcEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    PortraitField(form, eventSink)
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_npc),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_name),
        required = true,
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_race),
        value = form.race,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.RACE, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_race),
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_faction),
        value = form.faction,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.FACTION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_faction),
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_relationship),
        options = form.npcStatusOptions,
        onClick = { eventSink(CodexEvent.EntryNpcStatusChanged(it)) },
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_life),
        options = form.npcLifeOptions,
        onClick = { eventSink(CodexEvent.EntryNpcLifeChanged(it)) },
    )
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_where_to_meet),
        placeholder = stringResource(Res.string.codex_entry_pick_places_hint),
        onClick = { eventSink(CodexEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.NPC_PLACES)) },
        chips = form.npcPlaces,
    )
    FormTextArea(
        label = stringResource(Res.string.codex_entry_label_notes),
        value = form.description,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.DESCRIPTION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_notes),
    )
}

@Composable
private fun PartyEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    PartyIdentityFields(form, eventSink)
    PartyStatFields(form, eventSink)
}

@Composable
private fun PartyIdentityFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_owner),
        options = form.partyOwnerOptions,
        onClick = { eventSink(CodexEvent.EntryPartyOwnerChanged(it)) },
    )
    PortraitField(form, eventSink)
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_party),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_name),
        required = true,
    )
    // a player character's "вы" flag stands in for the player's name, so the field only shows for everyone else
    if (!form.partyIsPlayerCharacter) {
        FormTextField(
            label = stringResource(Res.string.codex_entry_label_player),
            value = form.partyPlayer,
            onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.PARTY_PLAYER, it)) },
            placeholder = stringResource(Res.string.codex_entry_placeholder_party_player),
        )
    }
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_class),
        value = form.partyClass,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.PARTY_CLASS, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_class),
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_race),
        value = form.race,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.RACE, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_race),
    )
}

@Composable
private fun PartyStatFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormNumberField(
        label = stringResource(Res.string.codex_entry_label_level),
        value = form.partyLevel,
        range = PartyStatRanges.level,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_LEVEL, it)) },
        action =
            form.partyLevelFromPlayer?.let {
                FormFieldAction(
                    label = stringResource(Res.string.codex_entry_party_level_from_player),
                    onClick = { eventSink(CodexEvent.EntryPartyLevelFromPlayerClicked) },
                )
            },
    )
    FormNumberField(
        label = stringResource(Res.string.codex_entry_label_hp_max),
        value = form.partyHpMax,
        range = PartyStatRanges.hpMax,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_HP_MAX, it)) },
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_presence),
        options = form.partyPresenceOptions,
        onClick = { eventSink(CodexEvent.EntryPartyPresenceChanged(it)) },
    )
    FormTextArea(
        label = stringResource(Res.string.codex_entry_label_notes),
        value = form.description,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.DESCRIPTION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_notes),
    )
}

@Composable
private fun QuestEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_quest),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_name),
        required = true,
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_giver),
        options = form.npcOptions,
        onClick = { eventSink(CodexEvent.EntryChipToggled(CodexEntryChipField.QUEST_GIVER, it)) },
        quickAdd = npcQuickAdd(form, eventSink, CodexEntryChipField.QUEST_GIVER),
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_reward),
        value = form.reward,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.REWARD, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_reward),
    )
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_where),
        placeholder = stringResource(Res.string.codex_entry_pick_place_hint),
        onClick = { eventSink(CodexEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.QUEST_PLACE)) },
        title = form.questPlace?.name,
        subtitle = form.questPlace?.path,
        leadingGlyph = MentionGlyph.LOCATION.symbol,
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_status),
        options = form.questStatusOptions,
        onClick = { eventSink(CodexEvent.EntryQuestStatusChanged(it)) },
    )
    FormTextArea(
        label = stringResource(Res.string.codex_entry_label_description),
        value = form.description,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.DESCRIPTION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_description),
    )
}

@Composable
private fun LocationEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_location),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_name),
        required = true,
    )
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_parent),
        placeholder = stringResource(Res.string.codex_entry_pick_place_hint),
        onClick = { eventSink(CodexEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.LOCATION_PARENT)) },
        title = form.locationParent?.name,
        subtitle = form.locationParent?.path,
        leadingGlyph = MentionGlyph.LOCATION.symbol,
    )
    FormTextArea(
        label = stringResource(Res.string.codex_entry_label_description),
        value = form.description,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.DESCRIPTION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_description),
    )
}

@Composable
private fun CodexEntryTypeSwitch(
    selected: CodexEntryType,
    onSelected: (CodexEntryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            CodexEntryType.PARTY to stringResource(Res.string.codex_entry_type_party),
            CodexEntryType.NPC to stringResource(Res.string.codex_entry_type_npc),
            CodexEntryType.QUEST to stringResource(Res.string.codex_entry_type_quest),
            CodexEntryType.LOCATION to stringResource(Res.string.codex_entry_type_location),
        )
    SegmentedControl(
        items = options,
        onSelected = { (type, _) -> onSelected(type) },
        itemBackground = { (type, _) ->
            if (type == selected) AppPalette.SurfaceElevated else Color.Transparent
        },
        modifier = modifier,
        containerBackground = AppPalette.Background,
        itemHeight = 40.dp,
    ) { (type, label) ->
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (type == selected) AppPalette.GoldBright else AppPalette.TextSecondary,
        )
    }
}

/**
 * the create/edit sheet with the place select and the "save changes?" prompt over it - shown by the codex and by the
 * session screen, which opens it prefilled for a mention whose record is gone. [toast] is the quick-add confirmation.
 */
@Composable
internal fun CodexEntryFormSheet(
    form: CodexEntryFormState,
    toast: String?,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = { eventSink(CodexEvent.EntryFormCloseRequested) },
        // the sheet stays up under the "save changes?" prompt instead of hiding first
        onDismissBlocked =
            if (form.hasUnsavedChanges) {
                { eventSink(CodexEvent.EntryFormCloseRequested) }
            } else {
                null
            },
    ) {
        Box {
            CodexEntryFormUi(
                form = form,
                eventSink = eventSink,
                onClose = { eventSink(CodexEvent.EntryFormCloseRequested) },
            )
            // the sheet covers the app-level toast, so the quick-add confirmation gets its own copy here
            AppToast(
                text = toast,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
    // a sheet of its own over the form, so dismissing it leaves the form where it was
    form.placeSelect?.let { select ->
        PlaceSelectSheet(
            select = select,
            onChosen = { eventSink(CodexEvent.EntryPlacesChosen(it)) },
            onCreated = { name, checked -> eventSink(CodexEvent.EntryPlaceCreated(name, checked)) },
            onDismiss = { eventSink(CodexEvent.EntryPlaceSelectClosed) },
        )
    }
    if (form.isDiscardPromptVisible) CodexEntryDiscardDialog(eventSink)
}

@Composable
private fun CodexEntryDiscardDialog(eventSink: (CodexEntryFormEvent) -> Unit) {
    // only two ways out - leave without saving or go back to the form, saving stays the form button's job
    ConfirmationDialog(
        title = stringResource(Res.string.codex_entry_discard_title),
        text = stringResource(Res.string.codex_entry_discard_text),
        onConfirm = { eventSink(CodexEvent.EntryFormDiscardPromptDismissed) },
        onDismiss = { eventSink(CodexEvent.EntryFormClosed) },
        confirmLabel = stringResource(Res.string.codex_entry_discard_back),
        dismissLabel = stringResource(Res.string.codex_entry_discard_leave),
        confirmColor = AppPalette.Gold,
        confirmContentColor = AppPalette.Background,
        icon = AppIcons.Edit,
        accent = AppPalette.Gold,
        accentBright = AppPalette.GoldBright,
        onDismissRequest = { eventSink(CodexEvent.EntryFormDiscardPromptDismissed) },
    )
}
