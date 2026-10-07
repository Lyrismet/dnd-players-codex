package com.lyrismet.incadent.core.swipehint

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** settings asks once to jump to the sessions tab and replay its swipe hint - AppTabHost consumes and clears it */
class SwipeHintReplayController {
    private val _sessionsReplayRequested = MutableStateFlow(false)
    val sessionsReplayRequested: StateFlow<Boolean> = _sessionsReplayRequested.asStateFlow()

    fun requestSessionsReplay() {
        _sessionsReplayRequested.value = true
    }

    fun onSessionsReplayHandled() {
        _sessionsReplayRequested.value = false
    }
}
