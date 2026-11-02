package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase

internal object JournalAction {
    const val ADDED = "ADDED"
    const val CHANGED = "CHANGED"
    const val ENTRY = "ENTRY"
    const val ALIAS = "ALIAS"
    const val PHOTO = "PHOTO"
    const val LABEL = "LABEL"
    const val COMBAT = "COMBAT"
}

/** collects what an import changes, so the last import into a campaign can be taken back */
internal class ImportJournal(
    private val enabled: Boolean,
) {
    private data class Row(
        val action: String,
        val kind: String,
        val rowId: Long,
        val ref: String,
        val snapshot: String,
        val flag: Long,
    )

    private val rows = mutableListOf<Row>()

    @Suppress("LongParameterList")
    private fun add(
        action: String,
        kind: String,
        rowId: Long = 0,
        ref: String = "",
        snapshot: String = "",
        flag: Boolean = false,
    ) {
        if (enabled) rows += Row(action, kind, rowId, ref, snapshot, if (flag) 1L else 0L)
    }

    /** a row the import wrote, [brought] when it came back from the list of what the person deleted */
    fun added(
        kind: String,
        id: Long,
        originId: String,
        brought: Boolean,
    ) = add(JournalAction.ADDED, kind, id, originId, flag = brought)

    fun changed(
        kind: String,
        id: Long,
        before: FileRecord,
    ) = add(JournalAction.CHANGED, kind, id, snapshot = FileJson.encodeToString(FileRecord.serializer(), before))

    fun entry(id: Long) = add(JournalAction.ENTRY, "ENTRY", id)

    fun combat(id: Long) = add(JournalAction.COMBAT, "COMBAT", id)

    fun alias(
        kind: String,
        aliasOriginId: String,
    ) = add(JournalAction.ALIAS, kind, ref = aliasOriginId)

    fun photo(
        owner: PhotoOwner,
        id: Long,
        before: String?,
    ) = add(JournalAction.PHOTO, owner.name, id, snapshot = before.orEmpty(), flag = before != null)

    /** the "я" mark moved off [previousId] (0 when nobody had it) */
    fun label(previousId: Long) = add(JournalAction.LABEL, "PARTY", previousId)

    /** replaces the journal of the campaign with this import's, an import that changed nothing keeps the old one */
    fun write(
        database: AppDatabase,
        campaignId: String,
    ) {
        if (!enabled || rows.isEmpty()) return
        val queries = database.importJournalQueries
        queries.clear(campaignId)
        rows.forEach { queries.insert(campaignId, it.action, it.kind, it.rowId, it.ref, it.snapshot, it.flag) }
    }
}
