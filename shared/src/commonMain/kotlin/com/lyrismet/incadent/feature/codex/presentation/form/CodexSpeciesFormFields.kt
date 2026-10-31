package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.attribution.SrdAttribution
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.feature.codex.domain.model.SPECIES_HIT_POINTS_RANGE
import com.lyrismet.incadent.feature.codex.presentation.summary.VulnerabilityGridEditor
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_creature_type
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_species
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_species_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_species_notes
import dndplayerscodex.shared.generated.resources.codex_entry_srd_label
import dndplayerscodex.shared.generated.resources.entity_sheet_species_hp_label
import dndplayerscodex.shared.generated.resources.entity_sheet_species_vulnerabilities_title
import org.jetbrains.compose.resources.stringResource

/** the species form with name and SRD hints, creature type, photo and notes */
@Composable
internal fun SpeciesEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    PortraitField(form, eventSink)
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_name_species),
        value = form.name,
        field = CodexEntryField.NAME,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_species_name),
        required = true,
    )
    if (form.srdSuggestions.isNotEmpty()) {
        FormChipPicker(
            label = stringResource(Res.string.codex_entry_srd_label),
            options =
                form.srdSuggestions.map {
                    FormChipOption(it, it.name, selected = false)
                },
            onClick = { eventSink(CodexEntryFormEvent.EntrySrdPicked(it.name, it.type)) },
        )
    }
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_creature_type),
        options = form.creatureTypeOptions,
        onClick = { eventSink(CodexEntryFormEvent.EntryCreatureTypeChanged(it)) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.entity_sheet_species_vulnerabilities_title).uppercase())
        VulnerabilityGridEditor(
            vulnerabilities = form.vulnerabilities,
            onChange = { type, state -> eventSink(CodexEntryFormEvent.EntryVulnerabilityChanged(type, state)) },
        )
    }
    CodexEntryNumberField(
        label = stringResource(Res.string.entity_sheet_species_hp_label),
        value = form.speciesHitPoints,
        range = SPECIES_HIT_POINTS_RANGE,
        field = CodexEntryNumberField.SPECIES_HIT_POINTS,
        eventSink = eventSink,
    )
    CodexEntryTextArea(
        label = stringResource(Res.string.codex_entry_label_notes),
        value = form.description,
        field = CodexEntryField.DESCRIPTION,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_species_notes),
    )
    SrdAttribution()
}
