package com.lyrismet.dndcodex.data.repository

import com.lyrismet.dndcodex.domain.model.AppLanguage
import com.lyrismet.dndcodex.domain.repository.LanguageRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val KEY_LANGUAGE = "app_language"

class LanguageRepositoryImpl(
    private val settings: ObservableSettings,
) : LanguageRepository {
    @OptIn(ExperimentalSettingsApi::class)
    override fun observeLanguage(): Flow<AppLanguage> =
        settings.getStringFlow(KEY_LANGUAGE, AppLanguage.RUSSIAN.tag).map { tag ->
            AppLanguage.entries.find { it.tag == tag } ?: AppLanguage.RUSSIAN
        }

    override fun setLanguage(language: AppLanguage) {
        settings.putString(KEY_LANGUAGE, language.tag)
    }
}
