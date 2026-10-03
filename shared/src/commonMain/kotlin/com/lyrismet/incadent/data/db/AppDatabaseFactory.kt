package com.lyrismet.incadent.data.db

import app.cash.sqldelight.db.SqlDriver
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Npc
import com.lyrismet.incadent.db.Quest
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.db.Session_note

fun createAppDatabase(driver: SqlDriver): AppDatabase {
    // SQLite ignores every ON DELETE CASCADE in the schema unless this is set per connection
    driver.execute(identifier = null, sql = "PRAGMA foreign_keys=ON;", parameters = 0)

    return AppDatabase(
        driver = driver,
        npcAdapter = Npc.Adapter(statusAdapter = npcStatusAdapter),
        questAdapter = Quest.Adapter(statusAdapter = questStatusAdapter),
        session_entryAdapter = Session_entry.Adapter(created_atAdapter = localDateTimeAdapter),
        session_noteAdapter =
            Session_note.Adapter(
                session_dateAdapter = localDateTimeAdapter,
                ended_atAdapter = localDateTimeAdapter,
            ),
    )
}
