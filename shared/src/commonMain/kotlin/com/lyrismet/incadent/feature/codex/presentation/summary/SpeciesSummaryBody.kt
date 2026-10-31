package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_species_creature_type_label
import dndplayerscodex.shared.generated.resources.entity_sheet_species_hp_label

@Composable
internal fun SpeciesSummaryBody(
    species: EntitySummaryItem.SpeciesSummary,
    actions: EntitySummarySheetActions,
) {
    val creatureType = creatureTypeLabels().getValue(species.creatureType)
    val creatureOptions =
        creatureTypeLabels().map { (type, label) ->
            StatusOption(type, label, SpeciesMentionColor, isSelected = type == species.creatureType)
        }
    val seen = speciesSeenCardLine(species.seenInSessions, species.fights)
    EntityHeader(
        emblem = species.emblem,
        title = species.name,
        subtitle = seen,
        showEditHint = actions.onEditClicked != null,
        quick = actions.quickEdit,
    )
    species.noteQuantity?.let { SpeciesNoteQuantityRow(it, actions.onSpeciesNoteQuantityStep) }
    EntityDescription(species.notes, actions.quickEdit)
    // the hit points are known from a fight or from the player, a dash until one of them says
    EntityFactsGrid(
        facts =
            listOf(
                FactRow(
                    Res.string.entity_sheet_species_creature_type_label,
                    FactValue.Text(creatureType),
                    QuickEditField.CREATURE_TYPE,
                    creatureOptions = creatureOptions,
                ),
                FactRow(
                    Res.string.entity_sheet_species_hp_label,
                    FactValue.Text(
                        species.hitPoints?.toString() ?: species.hitRange?.approxText().orDash(),
                    ),
                    QuickEditField.HIT_POINTS,
                ),
            ),
        onEntityRefClicked = actions.onEntityRefClicked,
        quick = actions.quickEdit,
    )
    SpeciesVulnerabilitySection(
        vulnerabilities = species.vulnerabilities,
        onChange =
            actions.onSpeciesVulnerabilityChanged?.let { changed ->
                { type, state -> changed(species.ref.id, type, state) }
            },
    )
    EntityRelatedNotesSection(species.name, species.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(species.ref, actions)
}
