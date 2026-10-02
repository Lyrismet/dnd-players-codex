package com.lyrismet.dndcodex.core.undo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val UNDO_WINDOW_MS = 5000L

data class UndoAction(
    val title: String,
    val subtitle: String,
    val onUndo: suspend () -> Unit,
)

/** shared across session/codex deletes - only one undo toast is on screen at a time */
class UndoController(
    private val scope: CoroutineScope,
) {
    private val _current = MutableStateFlow<UndoAction?>(null)
    val current: StateFlow<UndoAction?> = _current.asStateFlow()

    private var dismissJob: Job? = null

    fun show(
        title: String,
        subtitle: String,
        onUndo: suspend () -> Unit,
    ) {
        dismissJob?.cancel()
        _current.value = UndoAction(title, subtitle, onUndo)
        dismissJob =
            scope.launch {
                delay(UNDO_WINDOW_MS)
                _current.value = null
            }
    }

    fun undo() {
        val action = _current.value ?: return
        dismissJob?.cancel()
        _current.value = null
        scope.launch { action.onUndo() }
    }
}
