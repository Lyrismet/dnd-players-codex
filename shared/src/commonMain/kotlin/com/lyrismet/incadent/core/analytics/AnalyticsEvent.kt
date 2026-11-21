package com.lyrismet.incadent.core.analytics

import com.lyrismet.incadent.domain.model.EntityKind

/** every event the app reports - a fixed list with no free text, so no note or name can leak into analytics */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, String> get() = emptyMap()

    data class ScreenView(
        val screen: String,
    ) : AnalyticsEvent {
        override val name = "screen_view"
        override val params = mapOf("screen_name" to screen)
    }

    data class RecordCreated(
        val kind: EntityKind,
    ) : AnalyticsEvent {
        override val name = "record_created"
        override val params = mapOf("kind" to kind.name.lowercase())
    }

    data object CombatStarted : AnalyticsEvent {
        override val name = "combat_started"
    }

    data object CampaignImported : AnalyticsEvent {
        override val name = "campaign_imported"
    }

    data object CampaignExported : AnalyticsEvent {
        override val name = "campaign_exported"
    }
}
