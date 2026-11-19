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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** how long the bar waits for an undo, the countdown line of the toast runs for the same time */
const val UNDO_WINDOW_MILLIS = 5_000L

private val UNDO_WINDOW = UNDO_WINDOW_MILLIS.milliseconds
private const val LOG_COMPONENT = "UndoController"

/** what the bar reports, it picks the icon of the toast */
enum class UndoKind { Deleted, Made, Spent }

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
    val kind: UndoKind = if (followUp != null) UndoKind.Made else UndoKind.Deleted,
    // the toast draws its countdown from this mark, so it always runs out together with the controller's timer
    val shownAt: TimeMark = TimeSource.Monotonic.markNow(),
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
    ) = open(UndoAction(title, subtitle, onUndo))

    /** the bar of a spent slot or use, a check instead of the bin */
    fun showSpent(
        title: String,
        subtitle: String,
        onUndo: suspend () -> Unit,
    ) = open(UndoAction(title, subtitle, onUndo, kind = UndoKind.Spent))

    /** the bar of something just made, [followUp] being what to do with it next */
    fun showMade(
        title: String,
        subtitle: String,
        followUp: UndoFollowUp?,
        onUndo: suspend () -> Unit,
    ) = open(UndoAction(title, subtitle, onUndo, followUp, UndoKind.Made))

    private fun open(action: UndoAction) {
        dismissJob?.cancel()
        _current.value = action
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

    /** the bar is swiped away, nothing is undone and no follow-up runs */
    fun dismiss() {
        dismissJob?.cancel()
        _current.value = null
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
