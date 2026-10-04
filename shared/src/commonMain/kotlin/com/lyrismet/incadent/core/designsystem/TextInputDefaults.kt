package com.lyrismet.incadent.core.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

/** hand-rolled text fields capitalize sentences so the soft keyboard starts each one with a capital */
val SentenceKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
