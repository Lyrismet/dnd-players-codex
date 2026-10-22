package com.lyrismet.incadent.core.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import java.io.IOException

/** a file the system handed over as a content uri, only its bytes are read, the name is for display */
class UriFileOrigin(
    private val context: Context,
    private val uri: Uri,
) : FileOrigin {
    override val name: String = queryColumn(OpenableColumns.DISPLAY_NAME) { it.getString(0) }.orEmpty()

    override fun open(): FileSource {
        val size = queryColumn(OpenableColumns.SIZE) { if (it.isNull(0)) null else it.getLong(0) }
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("the file cannot be opened")
        return LineReaderSource(size, stream::read, stream::close)
    }

    private fun <T> queryColumn(
        column: String,
        read: (android.database.Cursor) -> T?,
    ): T? =
        runCatching {
            context.contentResolver
                .query(
                    uri,
                    arrayOf(column),
                    null,
                    null,
                    null,
                )?.use { if (it.moveToFirst()) read(it) else null }
        }.getOrNull()
}

/** the file another app opened or shared with this one, null for any other intent - nothing is read here */
fun Intent.sharedFileOrigin(context: Context): FileOrigin? {
    val uri =
        when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
    return uri?.let { UriFileOrigin(context.applicationContext, it) }
}
