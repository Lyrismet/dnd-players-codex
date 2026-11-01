package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Location

// caps the wrapper chain so a loop in the data still terminates
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

    /** the places from the top level one down to [place] itself, cut short where the chain loops */
    fun pathOf(place: Location): List<Location> =
        paths.getOrPut(place.id) {
            val chain = ArrayList<Location>()
            val seen = HashSet<Long>()
            var next: Location? = place
            while (next != null && seen.add(next.id)) {
                chain.add(next)
                next = byId[next.parentId]
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

    // the places just below the roots - the top level itself (a plane, a world) holds too much to be a region
    private val belowRoots: List<Location> by lazy { roots.flatMap { childrenOf(it.id) } }

    // the places down from [belowRoots] while each level holds exactly one place - wrappers such as a "continent"
    private val wrapperChain: List<Location> by lazy {
        val chain = ArrayList<Location>()
        var level = belowRoots
        while (level.size == 1 && chain.size < MAX_PATH_DEPTH) {
            val only = level.single()
            chain += only
            level = childrenOf(only.id)
        }
        chain
    }

    // the places a list is split into: the first level below the roots that holds several places, or the one wrapper
    private val sections: List<Location> by lazy {
        val last = wrapperChain.lastOrNull()
        if (last == null) belowRoots else childrenOf(last.id).ifEmpty { listOf(last) }
    }

    /**
     * the place a list section is titled with, null for a top level place and for a place with nothing below its
     * root - the top level (a plane, a world) is never a region. Going down from there, a place that holds exactly
     * one place is only a wrapper and is passed; the places where it first branches are the sections. A wrapper or
     * the branching place itself is listed under the section that sits right below it.
     */
    fun regionOf(place: Location): Location? {
        val path = pathOf(place)
        if (path.size < 2) return null
        return sections.firstOrNull { section -> path.any { it.id == section.id } } ?: wrapperChain.lastOrNull()
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

    /** why each place is out of reach as the parent of [id] - itself and everything inside it */
    fun blockedAsParent(id: Long): Map<Long, PlaceBlock> {
        val own = byId[id] ?: return emptyMap()
        return descendantIds(id).associateWith<Long, PlaceBlock> { PlaceBlock.InsideOf(own.name) } +
            (id to PlaceBlock.Itself)
    }

    /** the places the form already lists as inside [editing] and everything under them */
    fun pendingBlocks(editing: PlaceEditing): Map<Long, PlaceBlock> =
        editing.childIds
            .flatMap { listOf(it) + descendantIds(it) }
            .associateWith<Long, PlaceBlock> { PlaceBlock.Pending }

    /** why each place is out of reach as something inside [id] - itself and everything it sits in */
    fun blockedAsChild(id: Long): Map<Long, PlaceBlock> {
        val own = byId[id] ?: return emptyMap()
        return ancestorsOf(own).associate { it.id to PlaceBlock.Contains } + (id to PlaceBlock.Itself)
    }

    /** true when [id] may sit directly in [parentId] - the top level (null) always fits, a place or its inside never */
    fun canPutInside(
        id: Long,
        parentId: Long?,
    ): Boolean = parentId == null || (parentId != id && parentId !in descendantIds(id))

    /** which places must move under [parentId], and which leave it, for it to hold exactly [picked] afterwards */
    fun insideChanges(
        parentId: Long,
        picked: Collection<Long>,
    ): InsideChanges {
        val wanted = withoutCarried(picked).filter { canPutInside(it, parentId) }.toSet()
        val current = childrenOf(parentId).map { it.id }.toSet()
        return InsideChanges(entering = wanted - current, leaving = current - wanted)
    }

    /** [ids] without the places that already sit inside another one of them - those travel with it */
    fun withoutCarried(ids: Collection<Long>): List<Long> {
        val set = ids.toSet()
        return ids.distinct().filter { id ->
            val place = byId[id] ?: return@filter true
            ancestorsOf(place).none { it.id in set }
        }
    }
}

/** the places a "Внутри" pick moves in under a place and out to the top level */
data class InsideChanges(
    val entering: Set<Long>,
    val leaving: Set<Long>,
) {
    val isEmpty: Boolean get() = entering.isEmpty() && leaving.isEmpty()
}

/** the reason a place can't be picked for a move - [InsideOf] names the place being moved */
sealed interface PlaceBlock {
    data object Itself : PlaceBlock

    data class InsideOf(
        val name: String,
    ) : PlaceBlock

    data object Contains : PlaceBlock

    /** a place the form already lists as inside the moved one, so it can't become its parent */
    data object Pending : PlaceBlock
}

const val PATH_SEPARATOR = " › "
