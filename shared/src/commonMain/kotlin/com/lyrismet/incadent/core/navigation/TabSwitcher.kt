package com.lyrismet.incadent.core.navigation

import androidx.compose.runtime.compositionLocalOf

/** where a screen asks the app to show another tab, the app root provides it */
val LocalTabSwitcher = compositionLocalOf<(AppTab) -> Unit> { {} }
