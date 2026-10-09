package com.lyrismet.incadent

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.settings.SettingsFactory
import com.lyrismet.incadent.di.createAppGraph

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController() =
    ComposeUIViewController {
        val graph = remember { createAppGraph(DatabaseDriverFactory(), SettingsFactory()) }
        App(graph)
    }
