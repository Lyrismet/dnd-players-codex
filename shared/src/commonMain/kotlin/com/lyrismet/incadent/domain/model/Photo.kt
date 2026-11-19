package com.lyrismet.incadent.domain.model

/**
 * a stored picture, [full] is loaded only where a card or a crop needs it and its absence means "unchanged" on save,
 * [source] is the compressed picture before the crop, absent for a photo made before it was kept
 */
class Photo(
    val thumb: ByteArray,
    val full: ByteArray? = null,
    val source: ByteArray? = null,
) {
    /** the best picture this holder has - the full one when it was loaded */
    val display: ByteArray get() = full ?: thumb

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Photo &&
            thumb.contentEquals(other.thumb) &&
            full.contentEqualsNullable(other.full) &&
            source.contentEqualsNullable(other.source)

    override fun hashCode(): Int =
        31 * (31 * thumb.contentHashCode() + (full?.contentHashCode() ?: 0)) + (source?.contentHashCode() ?: 0)
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    if (this == null || other == null) this == null && other == null else contentEquals(other)
