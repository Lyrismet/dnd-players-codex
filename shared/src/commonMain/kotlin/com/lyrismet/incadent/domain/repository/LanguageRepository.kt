package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    fun setLanguage(language: AppLanguage)
}
