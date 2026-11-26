package com.lyrismet.incadent.core.analytics

import com.slack.circuit.runtime.CircuitUiEvent

// keystrokes of a text field, one report per letter would only drown the real taps
private val TypingSuffixes =
    listOf("TextChanged", "QueryChanged", "NameChanged", "DraftChanged", "NoteChanged", "TitleChanged")

/** reports what the user did on [screen] by the type of the event, its fields never reach analytics */
fun AnalyticsTracker.action(
    screen: String,
    event: CircuitUiEvent,
) {
    val name = event::class.simpleName ?: return
    if (TypingSuffixes.none(name::endsWith)) track(AnalyticsEvent.UiAction(screen, name))
}
