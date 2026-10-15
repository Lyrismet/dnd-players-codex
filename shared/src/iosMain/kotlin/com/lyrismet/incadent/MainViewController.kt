package com.lyrismet.incadent

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lyrismet.incadent.core.preferences.SettingsFactory
import com.lyrismet.incadent.di.createAppGraph
import com.lyrismet.incadent.persistence.DatabaseDriverFactory

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController() =
    ComposeUIViewController {
        val graph = remember { createAppGraph(DatabaseDriverFactory(), SettingsFactory()) }
        App(graph)
    }
