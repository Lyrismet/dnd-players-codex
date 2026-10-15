package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.grouping.toSelectOptions
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_parent
import dndplayerscodex.shared.generated.resources.codex_entry_label_where
import dndplayerscodex.shared.generated.resources.codex_entry_label_where_to_meet
import org.jetbrains.compose.resources.stringResource

/**
 * what the place select sheet shows for [field] - a place can't be put inside itself or inside what it contains, so
 * those are excluded when [editingLocationId] is the place whose parent is being picked
 */
@Suppress("LongParameterList")
internal fun placeSelectState(
    field: CodexEntryPlaceField,
    title: String,
    locations: List<Location>,
    selectedIds: List<Long>,
    editingLocationId: Long?,
    recentIds: List<Long>,
): PlaceSelectState {
    val tree = PlaceTree(locations)
    val excluded =
        if (field == CodexEntryPlaceField.LOCATION_PARENT && editingLocationId != null) {
            tree.descendantIds(editingLocationId) + editingLocationId
        } else {
            emptySet()
        }
    return PlaceSelectState(
        field = field,
        title = title,
        options = tree.toSelectOptions(locations),
        selectedIds = selectedIds.toSet(),
        multi = field == CodexEntryPlaceField.NPC_PLACES,
        excludedIds = excluded,
        recentIds = recentIds,
    )
}

/** what the field and its select sheet are called */
@Composable
internal fun placeFieldTitle(field: CodexEntryPlaceField): String =
    stringResource(
        when (field) {
            CodexEntryPlaceField.NPC_PLACES -> Res.string.codex_entry_label_where_to_meet
            CodexEntryPlaceField.QUEST_PLACE -> Res.string.codex_entry_label_where
            CodexEntryPlaceField.LOCATION_PARENT -> Res.string.codex_entry_label_parent
        },
    )

internal fun Location.toSelectedPlace(
    places: PlaceTree,
    topLevelLabel: String,
): SelectedPlace = SelectedPlace(name, places.ancestorsLabel(this).ifBlank { topLevelLabel })

internal fun CodexEntryFormFields.placeIds(field: CodexEntryPlaceField): List<Long> =
    when (field) {
        CodexEntryPlaceField.NPC_PLACES -> npcLocationIds
        CodexEntryPlaceField.QUEST_PLACE -> listOfNotNull(questLocationId)
        CodexEntryPlaceField.LOCATION_PARENT -> listOfNotNull(locationParentId)
    }

internal fun CodexEntryFormFields.withPlaces(
    field: CodexEntryPlaceField,
    ids: List<Long>,
): CodexEntryFormFields =
    when (field) {
        CodexEntryPlaceField.NPC_PLACES -> copy(npcLocationIds = ids.distinct())
        CodexEntryPlaceField.QUEST_PLACE -> copy(questLocationId = ids.firstOrNull())
        CodexEntryPlaceField.LOCATION_PARENT -> copy(locationParentId = ids.firstOrNull())
    }

/** the place field a held card fact edits and the places it holds now, null for a fact that is not a place */
internal fun quickPlaceTarget(
    ref: EntityRef?,
    records: CodexRecords,
): Pair<CodexEntryPlaceField, List<Long>>? =
    when (ref) {
        is EntityRef.Npc ->
            CodexEntryPlaceField.NPC_PLACES to
                records.npcs
                    .firstOrNull { it.id == ref.id }
                    ?.locationIds
                    .orEmpty()
        is EntityRef.Quest ->
            CodexEntryPlaceField.QUEST_PLACE to
                listOfNotNull(records.quests.firstOrNull { it.id == ref.id }?.locationId)
        is EntityRef.Location ->
            CodexEntryPlaceField.LOCATION_PARENT to
                listOfNotNull(records.locations.firstOrNull { it.id == ref.id }?.parentId)
        else -> null
    }
