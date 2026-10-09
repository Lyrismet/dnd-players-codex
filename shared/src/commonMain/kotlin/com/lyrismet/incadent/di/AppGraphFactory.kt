package com.lyrismet.incadent.di

import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.settings.SettingsFactory
import dev.zacsweers.metro.createGraphFactory

/** the single place the graph is built, so platform entry points never touch Metro directly */
fun createAppGraph(
    databaseDriverFactory: DatabaseDriverFactory,
    settingsFactory: SettingsFactory,
): AppGraph = createGraphFactory<AppGraph.Factory>().create(settingsFactory.createSettings(), databaseDriverFactory)
