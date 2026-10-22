package com.lyrismet.incadent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lyrismet.incadent.core.file.ExchangeRequest
import com.lyrismet.incadent.core.file.sharedFileOrigin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // the default exit fade would cross-fade two identical marks and show a faint seam, so cut instantly
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val graph = (application as IncadentApplication).graph
        // a recreated activity or a relaunch from recents must not open the same file again
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) openSharedFile(intent)

        setContent {
            App(graph, onExit = { finish() })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openSharedFile(intent)
    }

    // a campaign file tapped in another app or shared to this one goes to the import sheet
    private fun openSharedFile(intent: Intent?) {
        val origin = intent?.sharedFileOrigin(this) ?: return
        (application as IncadentApplication).graph.exchangeRequests.request(ExchangeRequest.Import(origin))
        // the same intent must not open the file again when the activity is recreated
        setIntent(Intent(Intent.ACTION_MAIN))
    }
}
