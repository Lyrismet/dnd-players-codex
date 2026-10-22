package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.core.preferences.SettingsFactory
import com.lyrismet.incadent.persistence.DatabaseDriverFactory
import dev.zacsweers.metro.createGraphFactory

/** the single place the graph is built, so platform entry points never touch Metro directly */
fun createAppGraph(
    databaseDriverFactory: DatabaseDriverFactory,
    settingsFactory: SettingsFactory,
    fileStorage: FileStorage,
): AppGraph =
    createGraphFactory<AppGraph.Factory>().create(settingsFactory.createSettings(), databaseDriverFactory, fileStorage)
