package com.lyrismet.incadent.core.language

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.preferences.LanguageRepository

sealed interface LanguageSheetEvent {
    data object Opened : LanguageSheetEvent

    data object Dismissed : LanguageSheetEvent

    data class Selected(
        val language: AppLanguage,
    ) : LanguageSheetEvent
}

/** open flag and the write of the language picker sheet */
class LanguageSheetController(
    private val repository: LanguageRepository,
) {
    val isOpen: MutableState<Boolean> = mutableStateOf(false)

    fun onEvent(event: LanguageSheetEvent) {
        when (event) {
            LanguageSheetEvent.Opened -> isOpen.value = true
            LanguageSheetEvent.Dismissed -> isOpen.value = false
            is LanguageSheetEvent.Selected -> {
                isOpen.value = false
                repository.setLanguage(event.language)
            }
        }
    }
}
