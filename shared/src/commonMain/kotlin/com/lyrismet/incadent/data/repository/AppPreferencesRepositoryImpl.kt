package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.HoldHintState
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val KEY_CAMPAIGN_NAME = "campaign_name"
private const val KEY_SESSION_NUMBERING = "session_numbering"
private const val KEY_MENTION_STYLE = "mention_style"
private const val KEY_ENTITY_EDIT_MODE = "entity_edit_mode"
private const val KEY_HOLD_HINT_STATE = "hold_hint_state"

class AppPreferencesRepositoryImpl(
    private val settings: ObservableSettings,
) : AppPreferencesRepository {
    @OptIn(ExperimentalSettingsApi::class)
    override fun observeCampaignName(): Flow<String?> = settings.getStringOrNullFlow(KEY_CAMPAIGN_NAME)

    override fun setCampaignName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) settings.remove(KEY_CAMPAIGN_NAME) else settings.putString(KEY_CAMPAIGN_NAME, trimmed)
    }

    @OptIn(ExperimentalSettingsApi::class)
    override fun observeSessionNumbering(): Flow<SessionNumbering> =
        settings.getStringFlow(KEY_SESSION_NUMBERING, SessionNumbering.ROMAN.tag).map { tag ->
            SessionNumbering.entries.find { it.tag == tag } ?: SessionNumbering.ROMAN
        }

    override fun setSessionNumbering(numbering: SessionNumbering) {
        settings.putString(KEY_SESSION_NUMBERING, numbering.tag)
    }

    @OptIn(ExperimentalSettingsApi::class)
    override fun observeMentionStyle(): Flow<MentionStyle> =
        settings.getStringFlow(KEY_MENTION_STYLE, MentionStyle.FILLED.tag).map { tag ->
            MentionStyle.entries.find { it.tag == tag } ?: MentionStyle.FILLED
        }

    override fun setMentionStyle(style: MentionStyle) {
        settings.putString(KEY_MENTION_STYLE, style.tag)
    }

    @OptIn(ExperimentalSettingsApi::class)
    override fun observeEntityEditMode(): Flow<EntityEditMode> =
        settings.getStringFlow(KEY_ENTITY_EDIT_MODE, EntityEditMode.QUICK.tag).map { tag ->
            EntityEditMode.entries.find { it.tag == tag } ?: EntityEditMode.QUICK
        }

    override fun setEntityEditMode(mode: EntityEditMode) {
        settings.putString(KEY_ENTITY_EDIT_MODE, mode.tag)
    }

    @OptIn(ExperimentalSettingsApi::class)
    override fun observeHoldHintState(): Flow<HoldHintState> =
        settings.getStringFlow(KEY_HOLD_HINT_STATE, HoldHintState.PENDING.tag).map { tag ->
            HoldHintState.entries.find { it.tag == tag } ?: HoldHintState.PENDING
        }

    override fun setHoldHintState(state: HoldHintState) {
        settings.putString(KEY_HOLD_HINT_STATE, state.tag)
    }
}
