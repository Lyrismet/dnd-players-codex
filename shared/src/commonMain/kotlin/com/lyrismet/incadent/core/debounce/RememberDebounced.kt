package com.lyrismet.incadent.core.debounce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

const val SEARCH_DEBOUNCE_MS = 400L

/** [value] after it stayed put for [delayMs], a value that [isImmediate] accepts goes through at once */
@Composable
fun <T> rememberDebounced(
    value: T,
    delayMs: Long = SEARCH_DEBOUNCE_MS,
    isImmediate: (T) -> Boolean = { false },
): T {
    val settled by produceState(value, value) {
        if (!isImmediate(value)) delay(delayMs)
        this.value = value
    }
    return settled
}
