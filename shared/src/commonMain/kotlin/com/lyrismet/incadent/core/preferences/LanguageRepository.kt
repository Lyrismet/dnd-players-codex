package com.lyrismet.incadent.core.preferences

import com.lyrismet.incadent.core.preferences.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    fun setLanguage(language: AppLanguage)
}
