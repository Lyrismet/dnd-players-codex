package com.lyrismet.dndcodex.domain.repository

import com.lyrismet.dndcodex.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    fun setLanguage(language: AppLanguage)
}
