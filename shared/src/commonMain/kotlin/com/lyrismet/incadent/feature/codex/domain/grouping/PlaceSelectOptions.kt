package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.feature.codex.domain.model.Location

/** the places as select options - the name is the title, the path above it tells same-named places apart */
fun PlaceTree.toSelectOptions(locations: List<Location>): List<SelectOption> =
    locations.map { place ->
        val path = ancestorsLabel(place)
        SelectOption(
            id = place.id,
            title = place.name,
            subtitle = path.ifBlank { null },
            searchText = if (path.isBlank()) place.name else "${place.name} $path",
        )
    }
