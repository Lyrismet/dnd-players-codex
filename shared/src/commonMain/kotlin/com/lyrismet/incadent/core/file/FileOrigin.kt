package com.lyrismet.incadent.core.file

/** a file that can be opened more than once, the preview reads it and the import reads it again */
interface FileOrigin {
    val name: String

    fun open(): FileSource
}
