package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.feature.mention.domain.insertMentionTrigger

/** the text and caret of a notes input, held outside it so a button next to the input can type "@" too */
@Stable
class MentionFieldState(
    text: String,
) {
    var value by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        internal set

    fun insertTrigger() {
        val inserted = insertMentionTrigger(value.text, value.selection.min, value.selection.max)
        value = TextFieldValue(inserted.text, TextRange(inserted.caret))
    }
}

@Composable
fun rememberMentionFieldState(text: String): MentionFieldState {
    val state = remember { MentionFieldState(text) }
    // a text changed from outside replaces the field's, an echo of what was typed is left alone
    LaunchedEffect(text) {
        if (text != state.value.text) state.value = TextFieldValue(text, TextRange(text.length))
    }
    return state
}
