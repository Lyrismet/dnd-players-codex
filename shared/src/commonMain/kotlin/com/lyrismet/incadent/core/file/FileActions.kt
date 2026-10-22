package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable

/** opens the system file picker, the file the person chose comes back through [onPicked] */
@Composable
expect fun rememberFilePicker(onPicked: (FileOrigin) -> Unit): () -> Unit

/** what the system can do with a finished export that sits in [FileStorage] under a file name */
class FileSharing(
    /** hands the cached export to the system share sheet */
    val share: (String) -> Unit,
    /** lets the person choose where to keep a copy of the cached export */
    val save: (String) -> Unit,
)

/** [onSaved] says whether the copy was written, a cancelled save dialog reports nothing */
@Composable
expect fun rememberFileSharing(onSaved: (Boolean) -> Unit): FileSharing
