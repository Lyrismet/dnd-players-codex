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

    @Test
    fun `migrating the second schema gives every row its own origin id and keeps the data`() {
        val file = File.createTempFile("schema-v2", ".db")
        File("src/commonMain/sqldelight/databases/2.db").copyTo(file, overwrite = true)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        listOf(
            "INSERT INTO campaign(id, name, created_at, last_opened_at) VALUES ('c1', 'One', 1, 1), ('c2', 'Two', 1, 1)",
            "INSERT INTO species(name, creature_type, campaign_id) VALUES ('Wolf', 'BEAST', 'c1'), ('Bear', 'BEAST', 'c1')",
            "INSERT INTO location(name, description, campaign_id) VALUES ('Town', '', 'c1'), ('Inn', '', 'c1')",
            "INSERT INTO npc(name, status, description, campaign_id) VALUES ('Bob', 'FRIEND', '', 'c1'), ('Ann', 'FRIEND', '', 'c1')",
            "INSERT INTO quest(title, status, reward, campaign_id) VALUES ('Find', 'ACTIVE', '', 'c1'), ('Lose', 'ACTIVE', '', 'c1')",
            "INSERT INTO party_member(name, campaign_id) VALUES ('Hero', 'c1'), ('Cleric', 'c1')",
            "INSERT INTO session_note(id, title, session_date, created_at, campaign_id) VALUES (1, 'A', '2026-01-01T00:00', 1, 'c1'), (2, 'B', '2026-01-02T00:00', 1, 'c1')",
            "INSERT INTO session_entry(session_note_id, created_at, body) VALUES (1, '2026-01-01T00:01', 'x'), (1, '2026-01-01T00:02', 'y')",
            "INSERT INTO combat(phase, started_at, campaign_id) VALUES ('FINISHED', '2026-01-01T00:00', 'c1'), ('FINISHED', '2026-01-01T00:00', 'c1')",
        ).forEach { driver.execute(null, it, 0) }

        AppDatabase.Schema.migrate(driver, 2, AppDatabase.Schema.version)

        listOf(
            "campaign",
            "species",
            "location",
            "npc",
            "quest",
            "party_member",
            "session_note",
            "session_entry",
            "combat",
        ).forEach { table ->
            assertEquals(
                "2",
                driver.single("SELECT COUNT(DISTINCT origin_id) FROM $table WHERE origin_id != ''"),
                "$table keeps its two rows and gives each its own origin id",
            )
            assertEquals("32", driver.single("SELECT MIN(length(origin_id)) FROM $table"), table)
        }
        assertEquals("Bob", driver.single("SELECT name FROM npc ORDER BY id LIMIT 1"))
        assertEquals("y", driver.single("SELECT body FROM session_entry ORDER BY id DESC LIMIT 1"))
        assertEquals("origin_alias", driver.single("SELECT name FROM sqlite_master WHERE name = 'origin_alias'"))
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
