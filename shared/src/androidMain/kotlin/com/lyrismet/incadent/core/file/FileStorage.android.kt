package com.lyrismet.incadent.core.file

import android.content.Context
import java.io.File

internal const val EXPORT_DIR = "exports"

actual class FileStorage(
    private val context: Context,
) {
    actual fun createSink(fileName: String): FileSink {
        val file = context.exportFile(fileName)
        file.parentFile?.listFiles()?.forEach { it.delete() }
        val stream = file.outputStream().buffered()
        return LineWriterSink(stream::write, stream::close)
    }
}

internal fun Context.exportFile(fileName: String): File =
    File(
        File(cacheDir, EXPORT_DIR).apply {
            mkdirs()
        },
        File(fileName).name,
    )
