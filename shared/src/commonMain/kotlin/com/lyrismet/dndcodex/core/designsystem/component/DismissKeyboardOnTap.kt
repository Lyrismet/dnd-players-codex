package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * tapping any descendant that doesn't itself consume the tap (a button, a text field, a list row's
 * clickable) clears focus and hides the software keyboard - apply once at the app root, RN's
 * `keyboardShouldPersistTaps` equivalent, since consumed taps (buttons, inputs, rows) never reach here
 */
fun Modifier.dismissKeyboardOnTap(): Modifier =
    composed {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            )
        }
    }
