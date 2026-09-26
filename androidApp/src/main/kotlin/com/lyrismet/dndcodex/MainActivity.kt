package com.lyrismet.dndcodex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lyrismet.dndcodex.data.AppContainer
import com.lyrismet.dndcodex.data.db.DatabaseDriverFactory
import com.lyrismet.dndcodex.data.settings.SettingsFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer =
            AppContainer(
                DatabaseDriverFactory(applicationContext),
                SettingsFactory(applicationContext),
            )

        setContent {
            App(appContainer)
        }
    }
}
