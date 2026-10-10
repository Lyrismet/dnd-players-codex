package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.withTimeoutOrNull

// well below the platform long press (about 400 ms) but above a normal tap, so the action shows up mid-hold
const val QUICK_HOLD_DELAY_MS = 200L

/** a tap fires [onTap] on release, a hold fires [onHold] after [holdDelayMs] with the finger down, never both */
fun Modifier.tapOrQuickHold(
    onTap: () -> Unit,
    onHold: () -> Unit,
    holdDelayMs: Long = QUICK_HOLD_DELAY_MS,
): Modifier =
    semantics {
        onClick(action = {
            onTap()
            true
        })
    }.pointerInput(onTap, onHold, holdDelayMs) {
        awaitEachGesture {
            awaitFirstDown()
            var released = false
            val finishedInTime =
                withTimeoutOrNull(holdDelayMs) {
                    released = waitForUpOrCancellation() != null
                    true
                }
            when {
                finishedInTime == null -> {
                    onHold()
                    waitForUpOrCancellation()
                }
                released -> onTap()
            }
        }
    }
