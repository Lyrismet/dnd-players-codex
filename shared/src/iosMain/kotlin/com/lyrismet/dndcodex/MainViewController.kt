package com.lyrismet.dndcodex

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lyrismet.dndcodex.data.AppContainer
import com.lyrismet.dndcodex.data.db.DatabaseDriverFactory

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController {
        val appContainer = remember { AppContainer(DatabaseDriverFactory()) }
        App(appContainer.circuit)
    }
