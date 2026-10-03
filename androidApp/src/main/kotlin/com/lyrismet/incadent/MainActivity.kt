package com.lyrismet.incadent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lyrismet.incadent.data.AppContainer
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.settings.SettingsFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // our own BrandMark is drawn pixel-identical to the launcher icon shown here, at the same spot
        // (see Splash.kt) - the system's default exit fade would cross-fade two near-identical gold
        // marks over each other, which reads as a faint seam, so cut instantly instead
        installSplashScreen().setOnExitAnimationListener { it.remove() }
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
