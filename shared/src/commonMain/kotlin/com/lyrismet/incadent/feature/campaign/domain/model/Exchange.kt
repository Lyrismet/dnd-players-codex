package com.lyrismet.incadent.feature.campaign.domain.model

import com.lyrismet.incadent.feature.campaign.domain.exchange.NoteOverlap
import kotlinx.datetime.LocalDate

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
    val mode: ExportMode = ExportMode.SELF,
    /** a friend copy carries the notes about the party only when asked */
    val partyNotes: Boolean = false,
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
    val mode: ExportMode = ExportMode.FRIEND,
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
    /** the answer for a record that looks like one already there, a missing one reads as [Resolution.LATER] */
    val resolutions: Map<ExchangeRef, Resolution> = emptyMap(),
    /** the choice per field of a record on both sides, a conflict not listed stays mine and a fill is taken */
    val fields: Map<ExchangeRef, Map<RecordField, FieldChoice>> = emptyMap(),
    /** records of the file with the name of one already there are the same record, off they come in as copies */
    val mergeNameTwins: Boolean = true,
    /** records this device deleted before that the person brought back */
    val restore: Set<ExchangeRef> = emptySet(),
    /** sessions of the file kept next to the one of the same day and title instead of merged into it */
    val separateSessions: Set<Long> = emptySet(),
    /** the mark put on a session kept separate, text is the caller's to translate */
    val fromFileLabel: String = "",
    /** a personal copy: its "я" mark becomes this campaign's own */
    val takeAsMine: Boolean = false,
    /** a personal copy: hit points, armor class and the notes about the party are taken too */
    val takeVitals: Boolean = false,
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

/** what an export writes: [SELF] is the full copy of a person's own campaign, [FRIEND] hides what is private */
enum class ExportMode { SELF, FRIEND }

/** how a record of a file is told apart from the ones already in the campaign */
enum class RecordStatus {
    NEW,
    SAME,
    CHANGED,
    SAME_NAME,
    SIMILAR,
    DELETED_BEFORE,
}

/** what a person decided about a record of the file that only looks like one already there */
enum class Resolution { SAME, APART, LATER }

/** which text of a field stays: the campaign's, the file's, or both one below the other (texts only) */
enum class FieldChoice { MINE, FILE, BOTH }

/**
 * a field both versions of a record have and that differs.
 * A [isFill] is a text the campaign has empty and the file fills, the rest are conflicts.
 */
data class FieldChange(
    val field: RecordField,
    val mine: String,
    val theirs: String,
    /** false for a status or a type, the values are then the names of the options, not text to show as they are */
    val isText: Boolean = true,
) {
    val isFill: Boolean get() = isText && mine.isBlank() && theirs.isNotBlank()
}

enum class RecordField {
    NAME,
    DESCRIPTION,
    STATUS,
    RACE,
    NICKNAMES,
    VULNERABILITIES,
    NOTES,
    HIT_POINTS,
    CHARACTER_CLASS,
    LEVEL,
    REWARD,
    LIFE_STATE,
    CREATURE_TYPE,
}

data class PlannedRecord(
    val ref: ExchangeRef,
    val name: String,
    val status: RecordStatus,
    /** the name of the record in the campaign a [RecordStatus.SIMILAR] one looks like */
    val similarTo: String? = null,
    val changes: List<FieldChange> = emptyList(),
)

enum class SessionStatus { NEW, SAME, MERGE }

data class PlannedSession(
    val fileId: Long,
    val title: String,
    val day: LocalDate,
    val status: SessionStatus,
    val overlap: NoteOverlap,
)

/**
 * the file is a personal copy: [characterName] is the one it marks as "я", null when it marks nobody, [ownCopy] when
 * that is the character this campaign already marks, so the file is the person's own backup and not someone else's
 */
data class PersonalCopy(
    val characterName: String?,
    val ownCopy: Boolean,
)

/** a file measured against the open campaign, nothing is written yet */
data class ImportPlan(
    val mode: ExportMode,
    val records: List<PlannedRecord>,
    val sessions: List<PlannedSession>,
    val personal: PersonalCopy?,
) {
    /** the ones a person has to answer for before the merge */
    val toResolve: List<PlannedRecord>
        get() = records.filter { it.status == RecordStatus.CHANGED || it.status == RecordStatus.SIMILAR }

    val deletedBefore: List<PlannedRecord> get() = records.filter { it.status == RecordStatus.DELETED_BEFORE }

    val newRecords: Int get() = records.count { it.status == RecordStatus.NEW }

    val nothingNew: Boolean
        get() =
            records.all {
                it.status == RecordStatus.SAME || (it.status == RecordStatus.SAME_NAME && it.changes.isEmpty())
            } &&
                sessions.all { it.status == SessionStatus.SAME }
}
