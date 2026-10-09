package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val KEY_LANGUAGE = "app_language"

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
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
