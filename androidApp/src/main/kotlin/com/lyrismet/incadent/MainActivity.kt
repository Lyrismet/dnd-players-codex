package com.lyrismet.incadent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lyrismet.incadent.core.preferences.SettingsFactory
import com.lyrismet.incadent.di.createAppGraph
import com.lyrismet.incadent.persistence.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // the default exit fade would cross-fade two identical marks and show a faint seam, so cut instantly
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val graph =
            createAppGraph(
                DatabaseDriverFactory(applicationContext),
                SettingsFactory(applicationContext),
            )

        setContent {
            App(graph, onExit = { finish() })
        }
    }
}
