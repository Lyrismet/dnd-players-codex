package com.lyrismet.incadent.data.db

import app.cash.sqldelight.ColumnAdapter
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime

val npcStatusAdapter = enumColumnAdapter<NpcStatus>()
val npcLifeStateAdapter = enumColumnAdapter<NpcLifeState>()
val partyPresenceAdapter = enumColumnAdapter<PartyPresence>()
val questStatusAdapter = enumColumnAdapter<QuestStatus>()

val localDateTimeAdapter =
    object : ColumnAdapter<LocalDateTime, String> {
        override fun decode(databaseValue: String): LocalDateTime = LocalDateTime.parse(databaseValue)

        override fun encode(value: LocalDateTime): String = value.toString()
    }

private inline fun <reified T : Enum<T>> enumColumnAdapter(): ColumnAdapter<T, String> =
    object : ColumnAdapter<T, String> {
        override fun decode(databaseValue: String): T = enumValueOf(databaseValue)

        override fun encode(value: T): String = value.name
    }
