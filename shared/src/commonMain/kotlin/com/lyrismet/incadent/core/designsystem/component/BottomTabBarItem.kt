package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class BottomTabBarItem(
    val label: String,
    // a short mark over the icon, e.g. the round of a fight in progress
    val badge: String? = null,
    val icon: @Composable (tint: Color) -> Unit,
)
