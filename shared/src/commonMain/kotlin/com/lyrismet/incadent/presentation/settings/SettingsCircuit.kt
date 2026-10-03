package com.lyrismet.incadent.presentation.settings

import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addSettingsUi(
    languageRepository: LanguageRepository,
    appPreferencesRepository: AppPreferencesRepository,
): Circuit.Builder =
    addPresenter<SettingsScreen, SettingsState> { _, _, _ ->
        SettingsPresenter(languageRepository, appPreferencesRepository)
    }.addUi<SettingsScreen, SettingsState> { state, modifier ->
        SettingsUi(state, modifier)
    }

val settingsScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SettingsScreen::class, SettingsScreen.serializer()) }
