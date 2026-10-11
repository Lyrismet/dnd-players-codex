package com.lyrismet.incadent.domain.repository

import kotlinx.coroutines.flow.Flow

/** the user's pinned phrase snippets ("Заготовки") in the order they were added */
interface PhraseSnippetRepository {
    fun observeAll(): Flow<List<String>>

    /** blank text and a phrase that is already pinned are ignored */
    suspend fun add(text: String)

    suspend fun remove(text: String)
}
