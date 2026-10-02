package com.lyrismet.dndcodex.presentation.codex

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
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.FormChipPicker
import com.lyrismet.dndcodex.core.designsystem.component.FormPrimaryButton
import com.lyrismet.dndcodex.core.designsystem.component.FormTextArea
import com.lyrismet.dndcodex.core.designsystem.component.FormTextField
import com.lyrismet.dndcodex.core.designsystem.component.SectionOverline
import com.lyrismet.dndcodex.core.designsystem.component.SegmentedControl
import com.lyrismet.dndcodex.core.designsystem.component.SheetCloseButton
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_description
import dndplayerscodex.shared.generated.resources.codex_entry_label_faction
import dndplayerscodex.shared.generated.resources.codex_entry_label_giver
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_location
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_npc
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_quest
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
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
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_quest_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_quest_reward
import dndplayerscodex.shared.generated.resources.codex_entry_type_location
import dndplayerscodex.shared.generated.resources.codex_entry_type_npc
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
                // matches every bottom-sheet body's content inset (Players Codex v5.dc.html sheetRef: 6px 20px 44px)
                .padding(top = 6.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // overline + heading sit flush against each other - the mockup gives this pair no gap of its own,
            // only the blocks around it get the 16dp rhythm
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
