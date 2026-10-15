package com.lyrismet.incadent.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.lyrismet.incadent.db.AppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            AppDatabase.Schema,
            "incadent_campaigns.db",
            // foreign keys are per connection, a pragma run once would miss the pooled ones
            onConfiguration = { config ->
                config.copy(extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true))
            },
        )
}
