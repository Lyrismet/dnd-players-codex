package com.lyrismet.incadent

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lyrismet.incadent.data.AppContainer
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.settings.SettingsFactory

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController() =
    ComposeUIViewController {
        val appContainer = remember { AppContainer(DatabaseDriverFactory(), SettingsFactory()) }
        App(appContainer)
    }
