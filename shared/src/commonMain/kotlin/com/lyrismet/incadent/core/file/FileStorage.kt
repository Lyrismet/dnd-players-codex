package com.lyrismet.incadent.core.file

/** the app's own cache folder, where an export is written before it is shared or saved */
expect class FileStorage {
    /** an empty file named [fileName] to write lines to, an older one of that name is replaced */
    fun createSink(fileName: String): FileSink
}
