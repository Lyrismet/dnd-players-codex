package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location

// caps the parent chain so a loop in the data still terminates
private const val MAX_PATH_DEPTH = 12
private const val SHORT_PATH_PLACES = 2

/** the place tree of one snapshot of the locations - paths are resolved once per place and only when asked for */
class PlaceTree(
    locations: List<Location>,
) {
    private val byId = locations.associateBy { it.id }
    private val childrenOf = locations.filter { it.parentId != null }.groupBy { it.parentId }

    /** places with no parent, or whose parent is not there - the first level of the tree */
    val roots: List<Location> = locations.filter { place -> place.parentId?.let { it in byId } != true }
    private val paths = HashMap<Long, List<Location>>()

    fun find(id: Long?): Location? = byId[id]

    /** the places from the top level one down to [place] itself, cut short where the parent chain loops */
    fun pathOf(place: Location): List<Location> =
        paths.getOrPut(place.id) {
            val chain = ArrayList<Location>()
            var next: Location? = place
            while (next != null && chain.size < MAX_PATH_DEPTH) {
                val current: Location = next
                if (chain.any { it.id == current.id }) break
                chain.add(current)
                next = byId[current.parentId]
            }
            chain.asReversed().toList()
        }

    /** the places above [place], top level first - empty for a top level place */
    fun ancestorsOf(place: Location): List<Location> = pathOf(place).dropLast(1)

    /** "A › B" of the places above [place], blank for a top level place */
    fun ancestorsLabel(place: Location): String = ancestorsOf(place).joinToString(PATH_SEPARATOR) { it.name }

    /** the path above [place] cut from the start to its last two places - "… › A › B" - blank at the top */
    fun shortAncestorsLabel(place: Location): String {
        val above = ancestorsOf(place).map { it.name }
        return if (above.size > SHORT_PATH_PLACES) {
            "… › " + above.takeLast(SHORT_PATH_PLACES).joinToString(PATH_SEPARATOR)
        } else {
            above.joinToString(PATH_SEPARATOR)
        }
    }

    /** the places directly inside [id] */
    fun childrenOf(id: Long): List<Location> = childrenOf[id].orEmpty()

    // the places down from the roots while each level holds exactly one place - wrappers such as a shared "world"
    private val wrapperChain: List<Location> by lazy {
        val chain = ArrayList<Location>()
        var level = roots
        while (level.size == 1 && chain.size < MAX_PATH_DEPTH) {
            val only = level.single()
            chain += only
            level = childrenOf(only.id)
        }
        chain
    }

    // the places a list is split into: the first level that holds several places, or the one wrapper if nothing does
    private val sections: List<Location> by lazy {
        val last = wrapperChain.lastOrNull()
        if (last == null) roots else childrenOf(last.id).ifEmpty { listOf(last) }
    }

    /**
     * the place a list section is titled with. Going down from the top, a place that holds exactly one place is only
     * a wrapper and is passed; the places where it first branches are the sections - so a shared "plane" with
     * several countries sections by country, and several top level places are sections themselves. A wrapper or
     * the branching place itself is listed under the section that sits right below it.
     */
    fun regionOf(place: Location): Location {
        val path = pathOf(place)
        val own = sections.firstOrNull { section -> path.any { it.id == section.id } }
        return own ?: wrapperChain.lastOrNull() ?: path.first()
    }

    /** true when [place] is [ancestorId] itself or somewhere inside it, at any depth */
    fun isInside(
        place: Location,
        ancestorId: Long,
    ): Boolean = pathOf(place).any { it.id == ancestorId }

    /** every place inside [id] at any depth, [id] itself excluded - what a parent picker must not offer */
    fun descendantIds(id: Long): Set<Long> {
        val result = LinkedHashSet<Long>()
        val queue = ArrayDeque(childrenOf[id].orEmpty())
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (result.add(next.id)) queue.addAll(childrenOf[next.id].orEmpty())
        }
        result.remove(id)
        return result
    }

    /** how many places sit one level in [id] */
    fun childCount(id: Long): Int = childrenOf[id]?.size ?: 0
}

const val PATH_SEPARATOR = " › "
