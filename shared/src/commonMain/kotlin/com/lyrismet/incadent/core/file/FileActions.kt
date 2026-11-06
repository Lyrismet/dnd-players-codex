package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable

/** opens the system file picker, the file the person chose comes back through [onPicked] */
@Composable
expect fun rememberFilePicker(onPicked: (FileOrigin) -> Unit): () -> Unit

/** hands a finished export that sits in [FileStorage] under a file name to the system share sheet */
@Composable
expect fun rememberFileSharing(): (String) -> Unit

/** a folder the person chose for a copy, [key] is what the platform needs to write into it and [name] what is shown */
data class SaveFolder(
    val key: String,
    val name: String,
)

/** what the system can do to keep a copy of a finished export */
class FileSaver(
    /** the system has a folder that takes a copy without asking, shown as the folder until another is chosen */
    val hasDefaultFolder: Boolean,
    /** opens the system folder picker, the answer comes through the `onFolder` of [rememberFileSaver] */
    val pickFolder: () -> Unit,
    /** copies the cached export [source] under the name [target] into [folder], the default one when it is null */
    val save: (source: String, target: String, folder: SaveFolder?) -> Unit,
)

/** [onSaved] says whether the copy was written, a cancelled system dialog reports nothing */
@Composable
expect fun rememberFileSaver(
    onFolder: (SaveFolder) -> Unit,
    onSaved: (Boolean) -> Unit,
): FileSaver
