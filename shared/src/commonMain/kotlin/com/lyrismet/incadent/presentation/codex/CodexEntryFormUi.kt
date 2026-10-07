package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormStepper
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.domain.model.PartyStatRanges
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_armor_class
import dndplayerscodex.shared.generated.resources.codex_entry_label_class
import dndplayerscodex.shared.generated.resources.codex_entry_label_description
import dndplayerscodex.shared.generated.resources.codex_entry_label_faction
import dndplayerscodex.shared.generated.resources.codex_entry_label_giver
import dndplayerscodex.shared.generated.resources.codex_entry_label_hp_max
import dndplayerscodex.shared.generated.resources.codex_entry_label_initiative_bonus
import dndplayerscodex.shared.generated.resources.codex_entry_label_level
import dndplayerscodex.shared.generated.resources.codex_entry_label_life
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_location
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_npc
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_party
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_quest
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
import dndplayerscodex.shared.generated.resources.codex_entry_label_owner
import dndplayerscodex.shared.generated.resources.codex_entry_label_player
import dndplayerscodex.shared.generated.resources.codex_entry_label_presence
import dndplayerscodex.shared.generated.resources.codex_entry_label_race
import dndplayerscodex.shared.generated.resources.codex_entry_label_region
import dndplayerscodex.shared.generated.resources.codex_entry_label_relationship
import dndplayerscodex.shared.generated.resources.codex_entry_label_reward
import dndplayerscodex.shared.generated.resources.codex_entry_label_status
import dndplayerscodex.shared.generated.resources.codex_entry_label_type
import dndplayerscodex.shared.generated.resources.codex_entry_label_where
import dndplayerscodex.shared.generated.resources.codex_entry_label_where_to_meet
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_description
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_region
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_location_type
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
    eventSink: (CodexEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
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
    eventSink: (CodexEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_npc),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_name),
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
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_where_to_meet),
        options = form.locationOptions,
        onClick = { eventSink(CodexEvent.EntryChipToggled(CodexEntryChipField.NPC_LOCATION, it)) },
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
    eventSink: (CodexEvent) -> Unit,
) {
    PartyIdentityFields(form, eventSink)
    PartyStatFields(form, eventSink)
}

@Composable
private fun PartyIdentityFields(
    form: CodexEntryFormState,
    eventSink: (CodexEvent) -> Unit,
) {
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_owner),
        options = form.partyOwnerOptions,
        onClick = { eventSink(CodexEvent.EntryPartyOwnerChanged(it)) },
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_party),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_party_name),
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
    eventSink: (CodexEvent) -> Unit,
) {
    FormStepper(
        label = stringResource(Res.string.codex_entry_label_level),
        value = form.partyLevel,
        range = PartyStatRanges.level,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_LEVEL, it)) },
        onOpenCalculator = { eventSink(CodexEvent.CalculatorOpened(CodexEntryNumberField.PARTY_LEVEL)) },
    )
    FormStepper(
        label = stringResource(Res.string.codex_entry_label_hp_max),
        value = form.partyHpMax,
        range = PartyStatRanges.hpMax,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_HP_MAX, it)) },
        onOpenCalculator = { eventSink(CodexEvent.CalculatorOpened(CodexEntryNumberField.PARTY_HP_MAX)) },
    )
    FormStepper(
        label = stringResource(Res.string.codex_entry_label_armor_class),
        value = form.partyArmorClass,
        range = PartyStatRanges.armorClass,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_ARMOR_CLASS, it)) },
        onOpenCalculator = { eventSink(CodexEvent.CalculatorOpened(CodexEntryNumberField.PARTY_ARMOR_CLASS)) },
    )
    // no calculator - its range starts below zero, matching the mockup's onPad `if (min < 0) return`
    FormStepper(
        label = stringResource(Res.string.codex_entry_label_initiative_bonus),
        value = form.partyInitiativeBonus,
        range = PartyStatRanges.initiativeBonus,
        onChange = { eventSink(CodexEvent.EntryNumberChanged(CodexEntryNumberField.PARTY_INITIATIVE_BONUS, it)) },
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
    eventSink: (CodexEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_quest),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_name),
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_giver),
        options = form.npcOptions,
        onClick = { eventSink(CodexEvent.EntryChipToggled(CodexEntryChipField.QUEST_GIVER, it)) },
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_reward),
        value = form.reward,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.REWARD, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_quest_reward),
    )
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_where),
        options = form.locationOptions,
        onClick = { eventSink(CodexEvent.EntryChipToggled(CodexEntryChipField.QUEST_LOCATION, it)) },
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
    eventSink: (CodexEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_name_location),
        value = form.name,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.NAME, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_name),
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_type),
        value = form.locationType,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.LOCATION_TYPE, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_type),
    )
    FormTextField(
        label = stringResource(Res.string.codex_entry_label_region),
        value = form.locationRegion,
        onChange = { eventSink(CodexEvent.EntryFieldChanged(CodexEntryField.LOCATION_REGION, it)) },
        placeholder = stringResource(Res.string.codex_entry_placeholder_location_region),
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
