package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** clears focus and hides the keyboard on any tap a descendant hasn't already consumed, apply once at the app root */
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
