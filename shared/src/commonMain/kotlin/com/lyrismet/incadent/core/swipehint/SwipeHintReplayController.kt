package com.lyrismet.incadent.core.swipehint

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** settings asks once to jump to the sessions tab and replay its swipe hint - AppTabHost consumes and clears it */
@Inject
@SingleIn(AppScope::class)
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
