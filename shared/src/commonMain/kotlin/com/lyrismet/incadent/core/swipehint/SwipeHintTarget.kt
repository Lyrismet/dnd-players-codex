package com.lyrismet.incadent.core.swipehint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.SwipeHintState

/** one playback's identity - the nonce forces a restart even when the same row/entity is targeted again */
data class SwipeHintTarget<T>(
    val itemId: T,
    val nonce: Int,
)

/** the shared presenter-side half of the one-time swipe hint - sessions and codex both call this */
@Composable
fun <T> rememberSwipeHintTarget(
    hintState: SwipeHintState,
    markSeen: () -> Unit,
    undoController: UndoController,
    firstItemId: T?,
    isBlocked: Boolean,
): SwipeHintTarget<T>? {
    val undoAction by undoController.current.collectAsState()
    val target = remember { mutableStateOf<SwipeHintTarget<T>?>(null) }
    // a bumped nonce forces rememberSwipeHintPlayback's key to change when the same row is targeted again
    val nonce = remember { mutableIntStateOf(0) }
    // guards against markSeen()'s write lagging a recomposition and re-firing while hintState is still PENDING
    val hasStartedForPending = remember { mutableStateOf(false) }
    LaunchedEffect(hintState, firstItemId, isBlocked, undoAction) {
        if (hintState != SwipeHintState.PENDING) {
            hasStartedForPending.value = false
            return@LaunchedEffect
        }
        if (hasStartedForPending.value) return@LaunchedEffect
        val eligible =
            shouldStartSwipeHint(
                hintState,
                hasFirstItem = firstItemId != null,
                isBlocked = isBlocked || undoAction != null,
            )
        if (eligible && firstItemId != null) {
            hasStartedForPending.value = true
            markSeen()
            nonce.intValue += 1
            target.value = SwipeHintTarget(firstItemId, nonce.intValue)
        }
    }
    return target.value
}
