package com.lyrismet.incadent.persistence

import app.cash.sqldelight.ColumnAdapter
import com.lyrismet.incadent.core.logging.AppLog
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlinx.datetime.LocalDateTime
import kotlin.enums.enumEntries

private const val LOG_COMPONENT = "Database"

val npcStatusAdapter = enumColumnAdapter(NpcStatus.NEUTRAL)
val creatureTypeAdapter = enumColumnAdapter(CreatureType.HUMANOID)
val npcLifeStateAdapter = enumColumnAdapter(NpcLifeState.ALIVE)
val partyPresenceAdapter = enumColumnAdapter(PartyPresence.IN)
val questStatusAdapter = enumColumnAdapter(QuestStatus.ACTIVE)
val campaignStatusAdapter = enumColumnAdapter(CampaignStatus.LIVE)
val exchangeKindAdapter = enumColumnAdapter(ExchangeKind.NPC)
val combatOutcomeAdapter = enumColumnAdapter(CombatOutcome.INTERRUPTED)
val combatPhaseAdapter = enumColumnAdapter(CombatPhase.FINISHED)
val combatSideAdapter = enumColumnAdapter(CombatSide.ENEMY)
val combatStanceAdapter = enumColumnAdapter(CombatStance.ENEMY)
val combatStateAdapter = enumColumnAdapter(CombatState.WHOLE)
val combatEventKindAdapter = enumColumnAdapter(CombatEventKind.DAMAGE)
val hitScaleAdapter = enumColumnAdapter(HitScale.ONE)
val hitEffectAdapter = enumColumnAdapter(HitEffect.FULL)
val damageTypeAdapter = enumColumnAdapter(DamageType.BLUDGEONING)

// a condition this build does not know is left out of the set instead of failing the row
val combatConditionsAdapter =
    object : ColumnAdapter<Set<CombatCondition>, String> {
        override fun decode(databaseValue: String): Set<CombatCondition> =
            databaseValue
                .split(',')
                .mapNotNull { name ->
                    CombatCondition.entries.firstOrNull { it.name == name }
                }.toSet()

        override fun encode(value: Set<CombatCondition>): String = value.joinToString(",") { it.name }
    }

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
