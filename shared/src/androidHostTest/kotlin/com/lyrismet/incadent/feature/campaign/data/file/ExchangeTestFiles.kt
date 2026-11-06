package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSource
import com.lyrismet.incadent.core.file.MemoryFile

/** a file written to memory as the origin an import reads from, every open starts at its first line */
internal fun origin(file: MemoryFile): FileOrigin =
    object : FileOrigin {
        override val name = "test.incadent.json"

        override fun open(): FileSource = MemoryFile(file.text.lines())
    }
