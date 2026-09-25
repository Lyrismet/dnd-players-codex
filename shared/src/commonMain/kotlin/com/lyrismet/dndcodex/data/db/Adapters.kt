package com.lyrismet.dndcodex.data.db

import app.cash.sqldelight.ColumnAdapter
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime

val npcStatusAdapter = enumColumnAdapter<NpcStatus>()
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
