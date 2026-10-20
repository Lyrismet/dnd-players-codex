package com.lyrismet.incadent.core.undo

import com.lyrismet.incadent.core.coroutines.AppCoroutineScope
import com.lyrismet.incadent.core.logging.AppLog
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val UNDO_WINDOW = 5.seconds
private const val LOG_COMPONENT = "UndoController"

/** a second button on the bar next to "Undo", for what to do with the thing that was just made */
data class UndoFollowUp(
    val label: String,
    val onClick: () -> Unit,
)

data class UndoAction(
    val title: String,
    val subtitle: String,
    val onUndo: suspend () -> Unit,
    val followUp: UndoFollowUp? = null,
    // a bar of something made keeps its icon after the follow-up is taken away
    val isMade: Boolean = followUp != null,
)

/** shared across session/codex deletes - only one undo toast is on screen at a time */
@Inject
@SingleIn(AppScope::class)
class UndoController(
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val _current = MutableStateFlow<UndoAction?>(null)
    val current: StateFlow<UndoAction?> = _current.asStateFlow()

    private var dismissJob: Job? = null

    fun show(
        title: String,
        subtitle: String,
        onUndo: suspend () -> Unit,
    ) = showMade(title, subtitle, followUp = null, onUndo)

    /** the bar of something just made, [followUp] being what to do with it next */
    fun showMade(
        title: String,
        subtitle: String,
        followUp: UndoFollowUp?,
        onUndo: suspend () -> Unit,
    ) {
        dismissJob?.cancel()
        _current.value = UndoAction(title, subtitle, onUndo, followUp)
        dismissJob =
            scope.launch {
                delay(UNDO_WINDOW)
                _current.value = null
            }
    }

    /** closes the bar without undoing and runs the follow-up button */
    fun followUp() {
        val action = _current.value ?: return
        dismissJob?.cancel()
        _current.value = null
        action.followUp?.onClick?.invoke()
    }

    /** takes the follow-up button off the bar when the screen that can act on it is gone, undo itself stays */
    fun dropFollowUp() {
        _current.update { it?.copy(followUp = null) }
    }

    fun undo() {
        val action = _current.value ?: return
        dismissJob?.cancel()
        _current.value = null
        // a failed restore must not crash the app from the shared scope
        scope.launch {
            runCatching { action.onUndo() }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                AppLog.error(LOG_COMPONENT, "undo failed", failure)
            }
        }
    }
}
