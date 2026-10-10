package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.withTimeoutOrNull

// shorter than the 450 ms press-and-hold that opens a quick editor, so releasing that hold never counts as a tap
private const val MAX_TAP_DURATION_MS = 250L

/** clears focus and hides the keyboard on a short tap a descendant hasn't already consumed, a hold is ignored */
fun Modifier.dismissKeyboardOnTap(): Modifier =
    composed {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                val up = withTimeoutOrNull(MAX_TAP_DURATION_MS) { waitForUpOrCancellation() }
                if (up != null) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
        }
    }
