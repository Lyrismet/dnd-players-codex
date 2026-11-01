package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.Photo

// no full jpeg means "unchanged" from a list-level model, and a picture equal to the stored one needs no rewrite
internal fun Photo.needsWrite(stored: Photo?): Boolean = full != null && !full.contentEquals(stored?.full)

// null removes the picture, see needsWrite for what leaves it alone
internal inline fun writePhotoIfChanged(
    photo: Photo?,
    stored: Photo?,
    set: (full: ByteArray?, thumb: ByteArray?) -> Unit,
) {
    when {
        photo == null -> if (stored != null) set(null, null)
        photo.needsWrite(stored) -> set(photo.full, photo.thumb)
    }
}
