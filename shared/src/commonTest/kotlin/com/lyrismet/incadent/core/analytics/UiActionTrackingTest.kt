package com.lyrismet.incadent.core.analytics

import com.slack.circuit.runtime.CircuitUiEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class UiActionTrackingTest {
    private sealed interface TestEvent : CircuitUiEvent {
        data object Saved : TestEvent

        data class TitleChanged(
            val text: String,
        ) : TestEvent
    }

    private class Recorder : AnalyticsTracker {
        val events = mutableListOf<AnalyticsEvent>()

        override fun track(event: AnalyticsEvent) {
            events += event
        }
    }

    @Test
    fun `a tap is reported by the type name without its fields`() {
        val recorder = Recorder()
        recorder.action("Test", TestEvent.Saved)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.UiAction("Test", "Saved")), recorder.events)
    }

    @Test
    fun `typing is not reported`() {
        val recorder = Recorder()
        recorder.action("Test", TestEvent.TitleChanged("secret"))
        assertEquals(emptyList(), recorder.events)
    }
}
