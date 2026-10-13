package com.lyrismet.incadent.core.navigation

import androidx.compose.runtime.compositionLocalOf

// a hidden tab stays fully composed (zero-sized) so its nav stack and ui state survive switching away - this
// tells its presenters whether their tab is actually on screen right now, e.g. so a one-time hint doesn't
// burn itself in the background while the user is looking at a different tab. Sheets and dialogs are windows of
// their own that ignore the zero size, so they check it as well and stay hidden while their tab is.
val LocalTabSelected = compositionLocalOf { true }
