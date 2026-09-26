package com.lyrismet.dndcodex

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lyrismet.dndcodex.data.AppContainer
import com.lyrismet.dndcodex.data.db.DatabaseDriverFactory
import com.lyrismet.dndcodex.data.settings.SettingsFactory

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController() =
    ComposeUIViewController {
        val appContainer = remember { AppContainer(DatabaseDriverFactory(), SettingsFactory()) }
        App(appContainer)
    }
