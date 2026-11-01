package com.lyrismet.incadent.feature.campaign.domain.model

/** one record of an export or of a file waiting to be imported, [subtitle] tells same-named ones apart */
data class ExchangeItem(
    val id: Long,
    val name: String,
    val subtitle: String = "",
)

/** everything that can be picked, the party counts as one list like the others */
data class ExchangeOptions(
    val sessions: List<ExchangeItem> = emptyList(),
    val places: List<ExchangeItem> = emptyList(),
    val npcs: List<ExchangeItem> = emptyList(),
    val quests: List<ExchangeItem> = emptyList(),
    val species: List<ExchangeItem> = emptyList(),
    val party: List<ExchangeItem> = emptyList(),
) {
    val recordCount: Int get() = places.size + npcs.size + quests.size + species.size + party.size
    val sessionCount: Int get() = sessions.size
}

/** the links between records, what decides which ones a pick drags along - ids are the export's or the file's own */
data class ExchangeGraph(
    val placeParent: Map<Long, Long?> = emptyMap(),
    val npcSpecies: Map<Long, Long?> = emptyMap(),
    val npcPlaces: Map<Long, List<Long>> = emptyMap(),
    val questGiver: Map<Long, Long?> = emptyMap(),
    val questPlace: Map<Long, Long?> = emptyMap(),
    val sessionMentions: Map<Long, Set<ExchangeRef>> = emptyMap(),
)

enum class ExchangeKind { SESSION, PLACE, NPC, QUEST, SPECIES, PARTY }

data class ExchangeRef(
    val kind: ExchangeKind,
    val id: Long,
)

/** the records a person ticked, whatever they need comes on its own (see [RecordScope]) */
data class RecordSelection(
    val sessions: Set<Long> = emptySet(),
    val places: Set<Long> = emptySet(),
    val npcs: Set<Long> = emptySet(),
    val quests: Set<Long> = emptySet(),
    val species: Set<Long> = emptySet(),
    val party: Set<Long> = emptySet(),
    val tags: Boolean = false,
    val phrases: Boolean = false,
    val photos: Boolean = true,
)

/** the ticked records and the ones they need, what an export writes and an import takes */
data class RecordScope(
    val sessions: Set<Long>,
    val places: Set<Long>,
    val npcs: Set<Long>,
    val quests: Set<Long>,
    val species: Set<Long>,
    val party: Set<Long>,
) {
    val recordCount: Int get() = places.size + npcs.size + quests.size + species.size + party.size
}

/** what the preview of a file shows */
data class ImportContents(
    val fileName: String,
    val campaignName: String,
    val bytes: Long,
    val withPhotos: Boolean,
    val options: ExchangeOptions,
    val graph: ExchangeGraph,
    val newer: NewerFile? = null,
)

/** a file made by a newer version of the app, [skipped] records of it cannot be read and are left out */
data class NewerFile(
    val fileVersion: Int,
    val appVersion: Int,
    val skipped: Int,
)

sealed interface ImportTarget {
    /** a campaign of its own, [name] is used when the file carries none */
    data class NewCampaign(
        val name: String,
    ) : ImportTarget

    data class Existing(
        val campaignId: String,
    ) : ImportTarget
}

/**
 * what happens to a record of the file that has the same name and kind as one already there: [SKIP] keeps the one
 * there (or replaces it, for the records named in [ImportRequest.replace]), [COPY] writes the file's under "(2)"
 */
enum class ConflictMode { SKIP, COPY }

data class ImportRequest(
    val target: ImportTarget,
    val conflicts: ConflictMode,
    val selection: RecordSelection? = null,
    /** what [selection] comes to once its dependencies are added, null takes the whole file */
    val scope: RecordScope? = null,
    /** the records of the file (kind and the file's own id) that overwrite the same-named record already there */
    val replace: Set<ExchangeRef> = emptySet(),
)

data class ImportResult(
    val campaignId: String,
    val records: Int,
    val sessions: Int,
    val partyAdded: Int = 0,
)

/** the file an export would make, in bytes - the text alone and what the photos add in base64 */
data class ExportEstimate(
    val textBytes: Long,
    val photoBytes: Long,
    val photoCount: Int,
)

/** what an export can pick from, with the links between the records */
data class ExchangeCatalog(
    val campaignName: String,
    val options: ExchangeOptions,
    val graph: ExchangeGraph,
)
