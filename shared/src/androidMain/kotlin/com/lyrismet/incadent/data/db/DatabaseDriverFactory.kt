package com.lyrismet.incadent.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lyrismet.incadent.db.AppDatabase

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver {
        // campaigns changed the schema without a migration, so the pre-campaign file is dropped and a new one is made
        context.deleteDatabase(LEGACY_DATABASE_NAME)
        return AndroidSqliteDriver(AppDatabase.Schema, context, DATABASE_NAME)
    }

    private companion object {
        const val LEGACY_DATABASE_NAME = "dnd_players_codex.db"
        const val DATABASE_NAME = "incadent_campaigns.db"
    }
}
