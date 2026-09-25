package com.lyrismet.dndcodex.data.db

import app.cash.sqldelight.db.SqlDriver
import com.lyrismet.dndcodex.db.AppDatabase
import com.lyrismet.dndcodex.db.Npc
import com.lyrismet.dndcodex.db.Quest
import com.lyrismet.dndcodex.db.Session_note

fun createAppDatabase(driver: SqlDriver): AppDatabase =
    AppDatabase(
        driver = driver,
        npcAdapter = Npc.Adapter(statusAdapter = npcStatusAdapter),
        questAdapter = Quest.Adapter(statusAdapter = questStatusAdapter),
        session_noteAdapter = Session_note.Adapter(session_dateAdapter = localDateTimeAdapter),
    )
