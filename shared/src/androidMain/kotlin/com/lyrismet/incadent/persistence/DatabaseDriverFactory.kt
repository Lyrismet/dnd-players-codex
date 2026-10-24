package com.lyrismet.incadent.persistence

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lyrismet.incadent.db.AppDatabase

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver {
        // campaigns changed the schema without a migration, so the pre-campaign file is dropped and a new one is made
        context.deleteDatabase(LEGACY_DATABASE_NAME)
        return AndroidSqliteDriver(
            AppDatabase.Schema,
            context,
            DATABASE_NAME,
            callback =
                object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                    // foreign keys are per connection, a pragma run once would miss the pooled ones
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        // the write-ahead log lets reads run beside a write and commits without a journal fsync
                        db.enableWriteAheadLogging()
                        db.setForeignKeyConstraintsEnabled(true)
                    }

                    // safe with the write-ahead log, a commit syncs at the checkpoint and not on every write
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA synchronous=NORMAL")
                    }
                },
        )
    }

    private companion object {
        const val LEGACY_DATABASE_NAME = "dnd_players_codex.db"
        const val DATABASE_NAME = "incadent_campaigns.db"
    }
}
