package com.lyrismet.incadent.core.file

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** the file waiting to be imported, a file opened from outside the app or picked in a sheet arrives here */
@Inject
@SingleIn(AppScope::class)
class ImportSource {
    private val state = MutableStateFlow<FileOrigin?>(null)
    val file: StateFlow<FileOrigin?> = state

    fun offer(origin: FileOrigin) {
        state.value = origin
    }

    fun clear() {
        state.value = null
    }
}
