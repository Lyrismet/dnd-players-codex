package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormFieldAction
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.QuickAddOutsideTapHost
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_discard_back
import dndplayerscodex.shared.generated.resources.codex_entry_discard_leave
import dndplayerscodex.shared.generated.resources.codex_entry_discard_text
import dndplayerscodex.shared.generated.resources.codex_entry_discard_title
import dndplayerscodex.shared.generated.resources.codex_entry_label_class
import dndplayerscodex.shared.generated.resources.codex_entry_label_description
import dndplayerscodex.shared.generated.resources.codex_entry_label_giver
import dndplayerscodex.shared.generated.resources.codex_entry_label_hp_max
import dndplayerscodex.shared.generated.resources.codex_entry_label_inside
import dndplayerscodex.shared.generated.resources.codex_entry_label_level
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_location
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_party
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_quest
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
import dndplayerscodex.shared.generated.resources.codex_entry_label_owner
import dndplayerscodex.shared.generated.resources.codex_entry_label_parent
import dndplayerscodex.shared.generated.resources.codex_entry_label_player
import dndplayerscodex.shared.generated.resources.codex_entry_label_presence
import dndplayerscodex.shared.generated.resources.codex_entry_label_race
import dndplayerscodex.shared.generated.resources.codex_entry_label_reward
import dndplayerscodex.shared.generated.resources.codex_entry_label_status
import dndplayerscodex.shared.generated.resources.codex_entry_label_where
import dndplayerscodex.shared.generated.resources.codex_entry_party_level_from_player
import dndplayerscodex.shared.generated.resources.codex_entry_pick_place_hint
import dndplayerscodex.shared.generated.resources.codex_entry_pick_places_hint
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_description
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_name
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
import dndplayerscodex.shared.generated.resources.codex_entry_type_species
import org.jetbrains.compose.resources.stringResource

private val CloseButtonSize = 40.dp

/** codex create/edit sheet body - renders [form]'s type-specific fields and dispatches through [eventSink] */
@Composable
internal fun CodexEntryFormUi(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    density: FormDensity = FormDensity.Full,
) {
    val focusManager = LocalFocusManager.current
    QuickAddOutsideTapHost(
        enabled = form.quickAddField != null,
        onOutsideTap = {
            focusManager.clearFocus()
            eventSink(CodexEntryFormEvent.EntryQuickAddCancelled)
        },
        modifier = modifier,
    ) {
        CodexEntryFormContent(form, eventSink, onClose, density)
        FormPhotoCropWindow(form, eventSink)
    }
}

@Composable
private fun CodexEntryFormContent(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    onClose: () -> Unit,
    density: FormDensity,
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
        AppSheetHeader(
            title = form.heading,
            onClose = onClose,
            overline = form.overline,
            closeSize = CloseButtonSize,
        )
        if (!form.isEditing) {
            CodexEntryTypeSwitch(
                selected = form.type,
                onSelected = { eventSink(CodexEntryFormEvent.EntryTypeChanged(it)) },
            )
        }
        when (form.type) {
            EntityKind.PARTY -> PartyEntryFields(form, eventSink)
            EntityKind.NPC -> NpcEntryFields(form, eventSink, density)
            EntityKind.QUEST -> QuestEntryFields(form, eventSink)
            EntityKind.LOCATION -> LocationEntryFields(form, eventSink)
            EntityKind.SPECIES -> SpeciesEntryFields(form, eventSink)
        }
        Text(
            stringResource(Res.string.codex_entry_required_note),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.TextSecondary,
        )
        FormPrimaryButton(
            text = form.saveLabel,
            onClick = { eventSink(CodexEntryFormEvent.EntryFormSaveClicked) },
            enabled = form.canSave,
        )
    }
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
        onClick = { eventSink(CodexEntryFormEvent.EntryPartyOwnerChanged(it)) },
    )
    PortraitField(form, eventSink)
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_name_party),
        value = form.name,
        field = CodexEntryField.NAME,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_name),
        required = true,
    )
    // a player character's "вы" flag stands in for the player's name, so the field only shows for everyone else
    if (!form.partyIsPlayerCharacter) {
        CodexEntryTextField(
            label = stringResource(Res.string.codex_entry_label_player),
            value = form.partyPlayer,
            field = CodexEntryField.PARTY_PLAYER,
            eventSink = eventSink,
            placeholder = stringResource(Res.string.codex_entry_placeholder_party_player),
        )
    }
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_class),
        value = form.partyClass,
        field = CodexEntryField.PARTY_CLASS,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_class),
    )
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_race),
        value = form.race,
        field = CodexEntryField.RACE,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_race),
    )
}

@Composable
private fun PartyStatFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CodexEntryNumberField(
        label = stringResource(Res.string.codex_entry_label_level),
        value = form.partyLevel,
        range = PartyStatRanges.level,
        field = CodexEntryNumberField.PARTY_LEVEL,
        eventSink = eventSink,
        action =
            form.partyLevelFromPlayer?.let {
                FormFieldAction(
                    label = stringResource(Res.string.codex_entry_party_level_from_player),
                    onClick = { eventSink(CodexEntryFormEvent.EntryPartyLevelFromPlayerClicked) },
                )
            },
    )
    CodexEntryNumberField(
        label = stringResource(Res.string.codex_entry_label_hp_max),
        value = form.partyHpMax,
        range = PartyStatRanges.hpMax,
        field = CodexEntryNumberField.PARTY_HP_MAX,
        eventSink = eventSink,
    )
    // a new party member is always in the party, the switch only matters when editing
    if (form.isEditing) {
        FormChipPicker(
            label = stringResource(Res.string.codex_entry_label_presence),
            options = form.partyPresenceOptions,
            onClick = { eventSink(CodexEntryFormEvent.EntryPartyPresenceChanged(it)) },
        )
    }
    CodexEntryTextArea(
        label = stringResource(Res.string.codex_entry_label_notes),
        value = form.description,
        field = CodexEntryField.DESCRIPTION,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_notes),
    )
}

@Composable
private fun QuestEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_name_quest),
        value = form.name,
        field = CodexEntryField.NAME,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_name),
        required = true,
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_giver),
        options = form.npcOptions,
        onClick = { eventSink(CodexEntryFormEvent.EntryChipToggled(CodexEntryChipField.QUEST_GIVER, it)) },
        quickAdd = npcQuickAdd(form, eventSink, CodexEntryChipField.QUEST_GIVER),
    )
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_reward),
        value = form.reward,
        field = CodexEntryField.REWARD,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_reward),
    )
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_where),
        placeholder = stringResource(Res.string.codex_entry_pick_place_hint),
        onClick = { eventSink(CodexEntryFormEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.QUEST_PLACE)) },
        title = form.questPlace?.name,
        subtitle = form.questPlace?.path,
        leadingGlyph = MentionGlyph.LOCATION.symbol,
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_status),
        options = form.questStatusOptions,
        onClick = { eventSink(CodexEntryFormEvent.EntryQuestStatusChanged(it)) },
    )
    CodexEntryTextArea(
        label = stringResource(Res.string.codex_entry_label_description),
        value = form.description,
        field = CodexEntryField.DESCRIPTION,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_description),
    )
}

@Composable
private fun LocationEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CoverField(form, eventSink)
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_name_location),
        value = form.name,
        field = CodexEntryField.NAME,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_name),
        required = true,
    )
    if (form.hasParentCandidates) {
        FormPickField(
            label = stringResource(Res.string.codex_entry_label_parent),
            placeholder = stringResource(Res.string.codex_entry_pick_place_hint),
            onClick = { eventSink(CodexEntryFormEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.LOCATION_PARENT)) },
            title = form.locationParent?.name,
            subtitle = form.locationParent?.path,
            leadingGlyph = MentionGlyph.LOCATION.symbol,
        )
        FormPickField(
            label = stringResource(Res.string.codex_entry_label_inside),
            placeholder = stringResource(Res.string.codex_entry_pick_places_hint),
            onClick = { eventSink(CodexEntryFormEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.LOCATION_CHILDREN)) },
            chips =
                form.locationChildren.takeIf { it.isNotEmpty() }?.let { places ->
                    { places.forEach { MentionChip(item = it, onClick = null) } }
                },
        )
    }
    CodexEntryTextArea(
        label = stringResource(Res.string.codex_entry_label_description),
        value = form.description,
        field = CodexEntryField.DESCRIPTION,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_description),
    )
}

@Composable
private fun CodexEntryTypeSwitch(
    selected: EntityKind,
    onSelected: (EntityKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            SegmentedTab(EntityKind.PARTY, stringResource(Res.string.codex_entry_type_party)),
            SegmentedTab(EntityKind.NPC, stringResource(Res.string.codex_entry_type_npc)),
            SegmentedTab(EntityKind.SPECIES, stringResource(Res.string.codex_entry_type_species)),
            SegmentedTab(EntityKind.QUEST, stringResource(Res.string.codex_entry_type_quest)),
            SegmentedTab(EntityKind.LOCATION, stringResource(Res.string.codex_entry_type_location)),
        )
    AppSegmentedTabs(
        tabs = options,
        selected = selected,
        onSelected = onSelected,
        modifier = modifier,
        containerBackground = AppPalette.Background,
        itemHeight = 40.dp,
    )
}

/** the create/edit sheet with the place and "save changes?" prompts over it, [density] picks the npc layout */
@Composable
internal fun CodexEntryFormSheet(
    form: CodexEntryFormState,
    toast: String?,
    density: FormDensity = FormDensity.Full,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = { eventSink(CodexEntryFormEvent.EntryFormCloseRequested) },
        // the sheet stays up under the "save changes?" prompt instead of hiding first
        onDismissBlocked =
            if (form.hasUnsavedChanges) {
                { eventSink(CodexEntryFormEvent.EntryFormCloseRequested) }
            } else {
                null
            },
    ) {
        Box {
            CodexEntryFormUi(
                form = form,
                eventSink = eventSink,
                onClose = { eventSink(CodexEntryFormEvent.EntryFormCloseRequested) },
                density = density,
            )
            // the sheet covers the app-level toast, so the quick-add confirmation gets its own copy here
            AppToast(
                text = toast,
                modifier = floatingBarAnchor(),
            )
        }
    }
    // a sheet of its own over the form, so dismissing it leaves the form where it was
    form.placeSelect?.let { select ->
        PlaceSelectSheet(
            select = select,
            onChosen = { eventSink(CodexEntryFormEvent.EntryPlacesChosen(it)) },
            onCreated = { name, checked -> eventSink(CodexEntryFormEvent.EntryPlaceCreated(name, checked)) },
            onDismiss = { eventSink(CodexEntryFormEvent.EntryPlaceSelectClosed) },
        )
    }
    form.speciesSelect?.let { select ->
        SpeciesSelectSheet(
            select = select,
            onChosen = { eventSink(CodexEntryFormEvent.EntrySpeciesChosen(it)) },
            onSrdChosen = { eventSink(CodexEntryFormEvent.EntrySpeciesSrdChosen(it.name, it.type)) },
            onCreated = { eventSink(CodexEntryFormEvent.EntrySpeciesCreated(it)) },
            onDismiss = { eventSink(CodexEntryFormEvent.EntrySpeciesSelectClosed) },
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
        onConfirm = { eventSink(CodexEntryFormEvent.EntryFormDiscardPromptDismissed) },
        onDismiss = { eventSink(CodexEntryFormEvent.EntryFormClosed) },
        confirmLabel = stringResource(Res.string.codex_entry_discard_back),
        dismissLabel = stringResource(Res.string.codex_entry_discard_leave),
        confirmTone = ButtonTone.Gold,
        icon = AppIcons.Edit,
        accent = AppPalette.Gold,
        accentBright = AppPalette.GoldBright,
        onDismissRequest = { eventSink(CodexEntryFormEvent.EntryFormDiscardPromptDismissed) },
    )
}
