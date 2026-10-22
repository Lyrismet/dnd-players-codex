package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_type_species
import dndplayerscodex.shared.generated.resources.codex_species_seen_format
import dndplayerscodex.shared.generated.resources.entity_sheet_species_creature_type_label
import dndplayerscodex.shared.generated.resources.entity_sheet_species_hp_label
import dndplayerscodex.shared.generated.resources.mention_count_in_sessions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SpeciesSummaryBody(
    species: EntitySummaryItem.SpeciesSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    val creatureType = creatureTypeLabels().getValue(species.creatureType)
    val creatureOptions =
        creatureTypeLabels().map { (type, label) ->
            StatusOption(type, label, SpeciesMentionColor, isSelected = type == species.creatureType)
        }
    val seen =
        if (species.seenInSessions > 0) {
            stringResource(
                Res.string.codex_species_seen_format,
                pluralStringResource(
                    Res.plurals.mention_count_in_sessions,
                    species.seenInSessions,
                    species.seenInSessions,
                ),
            )
        } else {
            ""
        }
    EntityHeader(
        emblem = species.emblem,
        overlineTypeLabel = stringResource(Res.string.codex_entry_type_species),
        overlineValue = creatureType,
        title = species.name,
        subtitle = seen,
        onClose = onClose,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(species.ref) } },
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
                nicknamesFact(species.nicknames),
                FactRow(
                    Res.string.entity_sheet_species_hp_label,
                    FactValue.Text(species.hitPoints.orDash()),
                    QuickEditField.HIT_POINTS,
                ),
            ),
        onEntityRefClicked = actions.onEntityRefClicked,
        quick = actions.quickEdit,
    )
    SpeciesDamageSection(
        damage = species.damage,
        onChange =
            actions.onSpeciesDamageChanged?.let { changed ->
                { type, state -> changed(species.ref.id, type, state) }
            },
    )
    EntityRelatedNotesSection(species.name, species.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(species.ref, actions)
}
