package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.selectlist.SelectCover
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.feature.codex.domain.model.Location

/** the id a place that is not saved yet stands under while its pickers are built */
const val NEW_PLACE_ID = 0L

/** a place as its form holds it - where it sits and what it holds, not yet written anywhere */
data class PlaceEditing(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val childIds: List<Long>,
)

/** the places as they will stand once [editing] is saved - the pickers check the rule against this tree */
fun List<Location>.withPending(editing: PlaceEditing): List<Location> {
    val children = editing.childIds.toSet()
    val moved =
        map { place ->
            when {
                place.id == editing.id -> place.copy(parentId = editing.parentId)
                place.id in children -> place.copy(parentId = editing.id)
                place.parentId == editing.id -> place.copy(parentId = null)
                else -> place
            }
        }
    return if (any { it.id == editing.id }) moved else moved + Location(editing.id, editing.name, "", editing.parentId)
}

/** the editing state of the stored [place] */
fun PlaceTree.editingOf(place: Location): PlaceEditing =
    PlaceEditing(place.id, place.name, place.parentId, childrenOf(place.id).map { it.id })

/**
 * the places as select options - the name is the title, the path above it tells same-named places apart.
 * [unavailable] holds the notes of the places that can't be picked, [carryAlong] lets a checked place carry the ones
 * inside it.
 */
fun PlaceTree.toSelectOptions(
    locations: List<Location>,
    unavailable: Map<Long, String> = emptyMap(),
    carryAlong: Boolean = false,
    topLevelCaption: String? = null,
): List<SelectOption> =
    locations.map { place ->
        val path = ancestorsLabel(place)
        SelectOption(
            id = place.id,
            title = place.name,
            subtitle = path.ifBlank { topLevelCaption },
            searchText = if (path.isBlank()) place.name else "${place.name} $path",
            unavailableNote = unavailable[place.id],
            coveredBy = if (carryAlong) ancestorsOf(place).map { SelectCover(it.id, it.name) } else emptyList(),
        )
    }
