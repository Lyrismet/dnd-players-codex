package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.mention.domain.MentionRef
import kotlinx.coroutines.flow.Flow

/** the mentions a player said "no" to as a link, kept per record so the question is not asked again */
interface LinkSuggestionDismissalRepository {
    fun observeDismissed(owner: MentionRef): Flow<Set<MentionRef>>

    suspend fun dismiss(
        owner: MentionRef,
        target: MentionRef,
    )

    /** takes a refusal back, used when the player undoes it */
    suspend fun restore(
        owner: MentionRef,
        target: MentionRef,
    )
}
