package com.lyrismet.incadent.persistence

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lyrismet.incadent.db.AppDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaMigrationTest {
    @Test
    fun `migrating the first schema keeps the rows and adds the indexes`() {
        val file = File.createTempFile("schema-v1", ".db")
        File("src/commonMain/sqldelight/databases/1.db").copyTo(file, overwrite = true)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        driver.execute(null, "INSERT INTO campaign(id, name, created_at, last_opened_at) VALUES ('c', 'Old', 1, 1)", 0)
        driver.execute(
            null,
            "INSERT INTO session_note(id, title, session_date, created_at, campaign_id) VALUES (1, 'S', '2026-01-01T00:00', 1, 'c')",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO session_entry(session_note_id, created_at, body) VALUES (1, '2026-01-01T00:01', 'kept')",
            0,
        )

        AppDatabase.Schema.migrate(driver, 1, AppDatabase.Schema.version)

        assertEquals("kept", driver.single("SELECT body FROM session_entry"))
        assertEquals(
            "combat_campaign_id",
            driver.single("SELECT name FROM sqlite_master WHERE name = 'combat_campaign_id'"),
        )
        assertEquals(
            "session_entry(session_note_id, created_at)",
            driver
                .single(
                    "SELECT sql FROM sqlite_master WHERE name = 'session_entry_session_note_id'",
                )?.substringAfter(" ON "),
        )
        driver.close()
        file.delete()
    }

    private fun SqlDriver.single(sql: String): String? =
        executeQuery(
            null,
            sql,
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
            0,
        ).value
}
