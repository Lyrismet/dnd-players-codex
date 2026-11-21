package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.analytics.AnalyticsTracker
import com.lyrismet.incadent.core.analytics.CrashReporter
import com.lyrismet.incadent.core.analytics.NoOpAnalyticsTracker
import com.lyrismet.incadent.core.analytics.NoOpCrashReporter
import com.lyrismet.incadent.core.appinfo.AppVersionReader
import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.core.preferences.SettingsFactory
import com.lyrismet.incadent.persistence.DatabaseDriverFactory
import dev.zacsweers.metro.createGraphFactory

/** the single place the graph is built, so platform entry points never touch Metro directly */
@Suppress("LongParameterList")
fun createAppGraph(
    databaseDriverFactory: DatabaseDriverFactory,
    settingsFactory: SettingsFactory,
    fileStorage: FileStorage,
    appVersionReader: AppVersionReader,
    analytics: AnalyticsTracker = NoOpAnalyticsTracker,
    crashReporter: CrashReporter = NoOpCrashReporter,
): AppGraph =
    createGraphFactory<AppGraph.Factory>()
        .create(
            settingsFactory.createSettings(),
            databaseDriverFactory,
            fileStorage,
            appVersionReader.read(),
            analytics,
            crashReporter,
        )
