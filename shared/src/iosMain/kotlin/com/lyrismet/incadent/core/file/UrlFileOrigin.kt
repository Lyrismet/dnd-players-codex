package com.lyrismet.incadent.core.file

import com.lyrismet.incadent.core.portrait.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingFromURL
import platform.Foundation.lastPathComponent
import platform.Foundation.readDataOfLength

/** a file at a local url, the picker copies it into the app first so no security scope has to be kept open */
class UrlFileOrigin(
    private val url: NSURL,
) : FileOrigin {
    override val name: String = url.lastPathComponent.orEmpty()

    @OptIn(ExperimentalForeignApi::class)
    override fun open(): FileSource {
        val path = url.path.orEmpty()
        val size =
            (
                NSFileManager.defaultManager
                    .attributesOfItemAtPath(
                        path,
                        error = null,
                    )?.get(NSFileSize) as? NSNumber
            )?.longLongValue
        val handle = NSFileHandle.fileHandleForReadingFromURL(url, error = null) ?: error("the file cannot be opened")
        return LineReaderSource(
            size,
            { buffer ->
                val data = handle.readDataOfLength(buffer.size.toULong()).toByteArray()
                if (data.isEmpty()) {
                    -1
                } else {
                    data.copyInto(buffer)
                    data.size
                }
            },
            { handle.closeFile() },
        )
    }
}
