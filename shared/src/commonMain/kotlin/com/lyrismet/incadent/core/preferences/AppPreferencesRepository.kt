package com.lyrismet.incadent.core.preferences

import com.lyrismet.incadent.core.preferences.EntityEditMode
import com.lyrismet.incadent.core.preferences.HoldHintState
import com.lyrismet.incadent.core.preferences.MentionStyle
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.preferences.SwipeHintState
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun observeSessionNumbering(): Flow<SessionNumbering>

    fun setSessionNumbering(numbering: SessionNumbering)

    fun observeMentionStyle(): Flow<MentionStyle>

    fun setMentionStyle(style: MentionStyle)

    fun observeEntityEditMode(): Flow<EntityEditMode>

    fun setEntityEditMode(mode: EntityEditMode)

    fun observeHoldHintState(): Flow<HoldHintState>

    fun setHoldHintState(state: HoldHintState)

    fun observeSessionListSwipeHintState(): Flow<SwipeHintState>

    fun setSessionListSwipeHintState(state: SwipeHintState)

    fun observeCodexSwipeHintState(): Flow<SwipeHintState>

    fun setCodexSwipeHintState(state: SwipeHintState)
}
