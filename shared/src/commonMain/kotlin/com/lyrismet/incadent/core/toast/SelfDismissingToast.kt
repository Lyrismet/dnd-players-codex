package com.lyrismet.incadent.core.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay

// shared by every screen-level toast - SessionList's rename confirmation, Codex's quick-add confirmation
const val TOAST_DURATION_MS = 3200L

/** nulls the toast back out after [TOAST_DURATION_MS] - a non-null value (even the same text) restarts the timer */
@Composable
fun <T> MutableState<T?>.autoDismiss() {
    LaunchedEffect(value) {
        if (value != null) {
            delay(TOAST_DURATION_MS)
            value = null
        }
    }
}
