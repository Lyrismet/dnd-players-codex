package com.lyrismet.incadent.persistence

import app.cash.sqldelight.ColumnAdapter
import com.lyrismet.incadent.core.logging.AppLog
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime
import kotlin.enums.enumEntries

private const val LOG_COMPONENT = "Database"

val npcStatusAdapter = enumColumnAdapter(NpcStatus.NEUTRAL)
val creatureTypeAdapter = enumColumnAdapter(CreatureType.HUMANOID)
val npcLifeStateAdapter = enumColumnAdapter(NpcLifeState.ALIVE)
val partyPresenceAdapter = enumColumnAdapter(PartyPresence.IN)
val questStatusAdapter = enumColumnAdapter(QuestStatus.ACTIVE)
val campaignStatusAdapter = enumColumnAdapter(CampaignStatus.LIVE)

private val FALLBACK_DATE_TIME = LocalDateTime.parse("1970-01-01T00:00")

// a value this build cannot read must not take the whole list down, it reads as the epoch and is logged
val localDateTimeAdapter =
    object : ColumnAdapter<LocalDateTime, String> {
        override fun decode(databaseValue: String): LocalDateTime =
            runCatching { LocalDateTime.parse(databaseValue) }.getOrElse {
                AppLog.warn(LOG_COMPONENT, "unreadable date time in a row, read as the epoch")
                FALLBACK_DATE_TIME
            }

        override fun encode(value: LocalDateTime): String = value.toString()
    }

// a stored value from a newer or older build reads as [fallback] instead of crashing every query that touches the row
internal inline fun <reified T : Enum<T>> enumColumnAdapter(fallback: T): ColumnAdapter<T, String> =
    object : ColumnAdapter<T, String> {
        override fun decode(databaseValue: String): T =
            enumEntries<T>().firstOrNull { it.name == databaseValue } ?: fallback.also {
                AppLog.warn(LOG_COMPONENT, "unknown ${T::class.simpleName} value in a row, read as ${it.name}")
            }

        override fun encode(value: T): String = value.name
    }
