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

/**
 * the shared presenter-side half of the one-time swipe hint - sessions and codex both call this once their list
 * and undo state are known, instead of each copy-pasting the same "pending + first row + nothing blocking" check
 */
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
    // a bumped nonce is the only thing that forces rememberSwipeHintPlayback's key to change when the same
    // row is the target again (e.g. "Показать" replayed with nothing else in the list having changed)
    val nonce = remember { mutableIntStateOf(0) }
    LaunchedEffect(hintState, firstItemId, isBlocked, undoAction) {
        val eligible =
            shouldStartSwipeHint(
                hintState,
                hasFirstItem = firstItemId != null,
                isBlocked = isBlocked || undoAction != null,
            )
        if (eligible && firstItemId != null) {
            markSeen()
            nonce.intValue += 1
            target.value = SwipeHintTarget(firstItemId, nonce.intValue)
        }
    }
    return target.value
}
