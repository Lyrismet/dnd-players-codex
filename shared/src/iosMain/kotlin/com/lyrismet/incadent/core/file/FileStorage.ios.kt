package com.lyrismet.incadent.core.file

import com.lyrismet.incadent.core.portrait.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.lastPathComponent
import platform.Foundation.writeData

private const val EXPORT_DIR = "exports"

internal fun exportPath(fileName: String): String =
    "${NSTemporaryDirectory()}$EXPORT_DIR/${NSURL.fileURLWithPath(fileName).lastPathComponent}"

actual class FileStorage {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createSink(fileName: String): FileSink {
        val manager = NSFileManager.defaultManager
        val dir = "${NSTemporaryDirectory()}$EXPORT_DIR"
        manager.removeItemAtPath(dir, error = null)
        manager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        val path = exportPath(fileName)
        manager.createFileAtPath(path, contents = null, attributes = null)
        val handle = NSFileHandle.fileHandleForWritingAtPath(path) ?: error("the export file cannot be opened")
        return LineWriterSink({ handle.writeData(it.toNSData()) }, { handle.closeFile() })
    }
}
