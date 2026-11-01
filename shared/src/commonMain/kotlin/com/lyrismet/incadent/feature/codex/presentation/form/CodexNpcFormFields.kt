package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.feature.codex.presentation.summary.NpcVulnerabilities
import com.lyrismet.incadent.feature.codex.presentation.summary.NpcVulnerabilityEditor
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_life
import dndplayerscodex.shared.generated.resources.codex_entry_label_name_npc
import dndplayerscodex.shared.generated.resources.codex_entry_label_notes
import dndplayerscodex.shared.generated.resources.codex_entry_label_relationship
import dndplayerscodex.shared.generated.resources.codex_entry_label_species
import dndplayerscodex.shared.generated.resources.codex_entry_label_where_to_meet
import dndplayerscodex.shared.generated.resources.codex_entry_pick_places_hint
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_name
import dndplayerscodex.shared.generated.resources.codex_entry_placeholder_npc_notes
import dndplayerscodex.shared.generated.resources.codex_species_pick_hint
import org.jetbrains.compose.resources.stringResource

/** the npc form, [FormDensity.Compact] folds the rarely filled fields under "more" */
@Composable
internal fun NpcEntryFields(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
    density: FormDensity,
) {
    when (density) {
        FormDensity.Full -> {
            PortraitField(form, eventSink)
            NpcNameField(form, eventSink)
            NpcSpeciesField(form, eventSink)
            NpcRelationField(form, eventSink)
            NpcLifeField(form, eventSink)
            NpcPlacesField(form, eventSink)
            NicknamesField(form, eventSink)
            NpcVulnerabilitiesField(form, eventSink)
            NpcNotesField(form, eventSink)
        }
        FormDensity.Compact -> {
            NpcNameField(form, eventSink)
            NpcRelationField(form, eventSink)
            NpcNotesField(form, eventSink)
            // the details open by themselves when the npc being edited already has something in them
            FormDetails(
                summary = npcDetailsSummary(form),
                startsOpen =
                    form.portrait != null ||
                        form.race.isNotBlank() ||
                        form.npcSpecies != null ||
                        form.nicknames.isNotBlank() ||
                        form.npcPlaces.isNotEmpty() ||
                        form.vulnerabilities.isNotEmpty(),
            ) {
                PortraitField(form, eventSink)
                NpcSpeciesField(form, eventSink)
                NpcLifeField(form, eventSink)
                NpcPlacesField(form, eventSink)
                NicknamesField(form, eventSink)
                NpcVulnerabilitiesField(form, eventSink)
            }
        }
    }
}

@Composable
private fun NpcNameField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CodexEntryTextField(
        label = stringResource(Res.string.codex_entry_label_name_npc),
        value = form.name,
        field = CodexEntryField.NAME,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_name),
        required = true,
    )
}

/** the race or species of the npc, picked from the bestiary in a select sheet that also creates one on the spot */
@Composable
private fun NpcSpeciesField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_species),
        // a race typed in an older version shows until a species replaces it
        placeholder = form.race.ifBlank { stringResource(Res.string.codex_species_pick_hint) },
        onClick = { eventSink(CodexEntryFormEvent.EntrySpeciesSelectOpened) },
        title = form.npcSpecies?.name,
        subtitle = form.npcSpecies?.typeLabel,
        leadingIcon = MentionGlyph.SPECIES.icon,
    )
}

@Composable
private fun NpcRelationField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_relationship),
        options = form.npcStatusOptions,
        onClick = { eventSink(CodexEntryFormEvent.EntryNpcStatusChanged(it)) },
    )
}

@Composable
private fun NpcLifeField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormChipPicker(
        label = stringResource(Res.string.codex_entry_label_life),
        options = form.npcLifeOptions,
        onClick = { eventSink(CodexEntryFormEvent.EntryNpcLifeChanged(it)) },
    )
}

@Composable
private fun NpcPlacesField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    FormPickField(
        label = stringResource(Res.string.codex_entry_label_where_to_meet),
        placeholder = stringResource(Res.string.codex_entry_pick_places_hint),
        onClick = { eventSink(CodexEntryFormEvent.EntryPlaceFieldOpened(CodexEntryPlaceField.NPC_PLACES)) },
        chips =
            form.npcPlaces.takeIf { it.isNotEmpty() }?.let { places ->
                { places.forEach { MentionChip(item = it, onClick = null) } }
            },
    )
}

@Composable
private fun NpcVulnerabilitiesField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    NpcVulnerabilityEditor(
        vulnerabilities =
            NpcVulnerabilities(
                form.vulnerabilities,
                form.npcSpeciesVulnerabilities,
                form.npcSpecies?.name,
            ),
        onChange = { type, state -> eventSink(CodexEntryFormEvent.EntryVulnerabilityChanged(type, state)) },
        onDone = null,
    )
}

@Composable
private fun NpcNotesField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    CodexEntryTextArea(
        label = stringResource(Res.string.codex_entry_label_notes),
        value = form.description,
        field = CodexEntryField.DESCRIPTION,
        eventSink = eventSink,
        placeholder = stringResource(Res.string.codex_entry_placeholder_npc_notes),
    )
}
