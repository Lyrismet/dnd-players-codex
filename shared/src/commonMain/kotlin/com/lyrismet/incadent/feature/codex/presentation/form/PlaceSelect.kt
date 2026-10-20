package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.NEW_PLACE_ID
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceBlock
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceEditing
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.grouping.editingOf
import com.lyrismet.incadent.feature.codex.domain.grouping.toSelectOptions
import com.lyrismet.incadent.feature.codex.domain.grouping.withPending
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.repository.PlaceCycleException
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_inside
import dndplayerscodex.shared.generated.resources.codex_entry_label_parent
import dndplayerscodex.shared.generated.resources.codex_entry_label_where
import dndplayerscodex.shared.generated.resources.codex_entry_label_where_to_meet
import dndplayerscodex.shared.generated.resources.codex_place_top_level_caption
import dndplayerscodex.shared.generated.resources.place_block_contains
import dndplayerscodex.shared.generated.resources.place_block_inside_of
import dndplayerscodex.shared.generated.resources.place_block_itself
import dndplayerscodex.shared.generated.resources.place_block_pending
import dndplayerscodex.shared.generated.resources.place_cycle_toast
import org.jetbrains.compose.resources.stringResource

/** what the place select sheet shows for [field], places [editing] can't sit in are greyed with the reason */
@Suppress("LongParameterList")
@Composable
internal fun placeSelectState(
    field: CodexEntryPlaceField,
    title: String,
    locations: List<Location>,
    selectedIds: List<Long>,
    editing: PlaceEditing?,
    recentIds: List<Long>,
): PlaceSelectState {
    val stored = PlaceTree(locations)
    val pending = if (editing != null) locations.withPending(editing) else locations
    val tree = PlaceTree(pending)
    val blocked =
        when {
            editing == null -> emptyMap()
            field == CodexEntryPlaceField.LOCATION_PARENT ->
                stored.pendingBlocks(editing) + stored.blockedAsParent(editing.id)
            field == CodexEntryPlaceField.LOCATION_CHILDREN -> tree.blockedAsChild(editing.id)
            else -> emptyMap()
        }
    val topLevel = stringResource(Res.string.codex_place_top_level_caption)
    return PlaceSelectState(
        field = field,
        title = title,
        options =
            tree.toSelectOptions(
                pending.filter { it.id != NEW_PLACE_ID },
                unavailable = blocked.mapValues { (_, block) -> block.text() },
                topLevelCaption = topLevel,
                carryAlong = field == CodexEntryPlaceField.LOCATION_CHILDREN,
            ),
        selectedIds = selectedIds.toSet(),
        multi = field.isMulti,
        recentIds = recentIds,
    )
}

@Composable
private fun PlaceBlock.text(): String =
    when (this) {
        PlaceBlock.Itself -> stringResource(Res.string.place_block_itself)
        PlaceBlock.Pending -> stringResource(Res.string.place_block_pending)
        is PlaceBlock.InsideOf -> stringResource(Res.string.place_block_inside_of, name)
        PlaceBlock.Contains -> stringResource(Res.string.place_block_contains)
    }

/** the stored [place] as the pickers see it, null for something that is not a place */
internal fun editingPlace(
    ref: EntityRef?,
    locations: List<Location>,
): PlaceEditing? =
    PlaceTree(locations).let { tree -> tree.find((ref as? EntityRef.Location)?.id)?.let(tree::editingOf) }

/** what the field and its select sheet are called */
@Composable
internal fun placeFieldTitle(field: CodexEntryPlaceField): String =
    stringResource(
        when (field) {
            CodexEntryPlaceField.NPC_PLACES -> Res.string.codex_entry_label_where_to_meet
            CodexEntryPlaceField.QUEST_PLACE -> Res.string.codex_entry_label_where
            CodexEntryPlaceField.LOCATION_PARENT -> Res.string.codex_entry_label_parent
            CodexEntryPlaceField.LOCATION_CHILDREN -> Res.string.codex_entry_label_inside
        },
    )

internal fun Location.toSelectedPlace(
    places: PlaceTree,
    topLevelLabel: String,
): SelectedPlace = SelectedPlace(name, places.ancestorsLabel(this).ifBlank { topLevelLabel })

/** the place being filled in as the pickers see it, null while the form holds some other kind of record */
internal fun CodexEntryFormFields.placeEditing(): PlaceEditing? =
    if (type == EntityKind.LOCATION) {
        PlaceEditing(
            id = (editingRef as? EntityRef.Location)?.id ?: NEW_PLACE_ID,
            name = name,
            parentId = locationParentId,
            childIds = locationChildIds,
        )
    } else {
        null
    }

internal fun CodexEntryFormFields.placeIds(field: CodexEntryPlaceField): List<Long> =
    when (field) {
        CodexEntryPlaceField.NPC_PLACES -> npcLocationIds
        CodexEntryPlaceField.QUEST_PLACE -> listOfNotNull(questLocationId)
        CodexEntryPlaceField.LOCATION_PARENT -> listOfNotNull(locationParentId)
        CodexEntryPlaceField.LOCATION_CHILDREN -> locationChildIds
    }

internal fun CodexEntryFormFields.withPlaces(
    field: CodexEntryPlaceField,
    ids: List<Long>,
): CodexEntryFormFields =
    when (field) {
        CodexEntryPlaceField.NPC_PLACES -> copy(npcLocationIds = ids.distinct())
        CodexEntryPlaceField.QUEST_PLACE -> copy(questLocationId = ids.firstOrNull())
        CodexEntryPlaceField.LOCATION_PARENT -> copy(locationParentId = ids.firstOrNull())
        CodexEntryPlaceField.LOCATION_CHILDREN -> copy(locationChildIds = ids.distinct())
    }

/** the place field a held card fact edits, null for a card without one */
internal fun quickPlaceField(
    ref: EntityRef?,
    held: QuickEditField,
): CodexEntryPlaceField? =
    when (ref) {
        is EntityRef.Npc -> CodexEntryPlaceField.NPC_PLACES
        is EntityRef.Quest -> CodexEntryPlaceField.QUEST_PLACE
        is EntityRef.Location ->
            if (held == QuickEditField.INSIDE) {
                CodexEntryPlaceField.LOCATION_CHILDREN
            } else {
                CodexEntryPlaceField.LOCATION_PARENT
            }
        else -> null
    }

/** the place field a held card fact edits and the places it holds now, null for a fact that is not a place */
internal fun quickPlaceTarget(
    ref: EntityRef?,
    held: QuickEditField,
    records: CodexRecords,
): Pair<CodexEntryPlaceField, List<Long>>? {
    val field = quickPlaceField(ref, held) ?: return null
    val ids =
        when (ref) {
            is EntityRef.Npc ->
                records.npcs
                    .firstOrNull { it.id == ref.id }
                    ?.locationIds
                    .orEmpty()
            is EntityRef.Quest -> listOfNotNull(records.quests.firstOrNull { it.id == ref.id }?.locationId)
            is EntityRef.Location ->
                if (field == CodexEntryPlaceField.LOCATION_CHILDREN) {
                    records.locations.filter { it.parentId == ref.id }.map { it.id }
                } else {
                    listOfNotNull(records.locations.firstOrNull { it.id == ref.id }?.parentId)
                }
            else -> emptyList()
        }
    return field to ids
}

/** the toast text for a failure a place move is known to cause, null for any other failure */
@Composable
fun rememberPlaceFailureText(): (Throwable) -> String? {
    val cycle = stringResource(Res.string.place_cycle_toast)
    return { failure -> cycle.takeIf { failure is PlaceCycleException } }
}
