package com.lyrismet.incadent

import android.app.Application
import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.core.preferences.SettingsFactory
import com.lyrismet.incadent.di.AppGraph
import com.lyrismet.incadent.di.createAppGraph
import com.lyrismet.incadent.persistence.DatabaseDriverFactory

/** owns the DI graph, so a rotated or recreated activity reuses the open database and the single-instance state */
class IncadentApplication : Application() {
    val graph: AppGraph by lazy {
        createAppGraph(
            DatabaseDriverFactory(applicationContext),
            SettingsFactory(applicationContext),
            FileStorage(applicationContext),
        )
    }
}
