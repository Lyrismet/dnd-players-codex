package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.jpegDimensions
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import kotlin.io.encoding.Base64

// a phone photo is cut to a few hundred pixels on save, a file that claims more than this is not one of ours
private const val MAX_PHOTO_SIDE_PX = 4000

/** puts the photos of a file on the records the import wrote, ids maps the file's ids to the new ones per owner */
internal class ImportPhotos(
    private val database: AppDatabase,
    private val makeThumb: (ByteArray, PhotoSlot) -> ByteArray,
    private val journal: ImportJournal,
    private val ids: Map<PhotoOwner, Map<Long, Long>>,
) {
    // only records written by this import take a photo, a matched one keeps what it has
    val fresh = mutableSetOf<Pair<PhotoOwner, Long>>()
    val overwritten = mutableSetOf<Pair<PhotoOwner, Long>>()

    fun add(record: FileRecord.Photo) {
        if ((record.owner to record.ownerId) !in fresh) return
        val bytes = decodeJpeg(record.data)
        val id = ids.getValue(record.owner)[record.ownerId]
        if (bytes != null && id != null) write(record, id, bytes)
    }

    private fun write(
        record: FileRecord.Photo,
        id: Long,
        bytes: ByteArray,
    ) {
        val slot = if (record.owner == PhotoOwner.PLACE) PhotoSlot.COVER else PhotoSlot.PORTRAIT
        val thumb = makeThumb(bytes, slot)
        remember(record, id)
        when (record.owner) {
            PhotoOwner.SPECIES -> database.speciesQueries.setPhoto(bytes, thumb, null, id)
            PhotoOwner.PLACE -> database.locationQueries.setCover(bytes, thumb, null, id)
            PhotoOwner.NPC -> database.npcQueries.setPortrait(bytes, thumb, null, id)
            PhotoOwner.PARTY -> database.partyMemberQueries.setPortrait(bytes, thumb, null, id)
            PhotoOwner.ITEM -> database.itemQueries.setPhoto(bytes, thumb, null, id)
        }
    }

    // a photo that replaces another one is kept in the journal, so taking the import back can restore it
    private fun remember(
        record: FileRecord.Photo,
        id: Long,
    ) {
        if ((record.owner to record.ownerId) !in overwritten) return
        val current: ByteArray? =
            when (record.owner) {
                PhotoOwner.SPECIES ->
                    database.speciesQueries
                        .selectPhoto(id)
                        .executeAsOneOrNull()
                        ?.photo
                PhotoOwner.PLACE ->
                    database.locationQueries
                        .selectPhoto(id)
                        .executeAsOneOrNull()
                        ?.cover
                PhotoOwner.NPC ->
                    database.npcQueries
                        .selectPhoto(id)
                        .executeAsOneOrNull()
                        ?.portrait
                PhotoOwner.PARTY ->
                    database.partyMemberQueries
                        .selectPhoto(id)
                        .executeAsOneOrNull()
                        ?.portrait
                PhotoOwner.ITEM ->
                    database.itemQueries
                        .selectPhoto(id)
                        .executeAsOneOrNull()
                        ?.photo
            }
        journal.photo(record.owner, id, current?.let { Base64.encode(it) })
    }

    // only a jpeg of a sane size is decoded, a picture that is not one is dropped and the record stays without it
    private fun decodeJpeg(text: String): ByteArray? {
        val bytes =
            try {
                Base64.decode(text)
            } catch (_: IllegalArgumentException) {
                throw ImportFailedException(ImportError.Malformed)
            }
        val (width, height) = jpegDimensions(bytes) ?: return null
        return bytes.takeIf { width <= MAX_PHOTO_SIDE_PX && height <= MAX_PHOTO_SIDE_PX }
    }
}
