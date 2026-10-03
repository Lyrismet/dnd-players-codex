package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    /** null means the campaign has never been renamed, so the default name applies */
    fun observeCampaignName(): Flow<String?>

    fun setCampaignName(name: String)

    fun observeSessionNumbering(): Flow<SessionNumbering>

    fun setSessionNumbering(numbering: SessionNumbering)

    fun observeMentionStyle(): Flow<MentionStyle>

    fun setMentionStyle(style: MentionStyle)
}
