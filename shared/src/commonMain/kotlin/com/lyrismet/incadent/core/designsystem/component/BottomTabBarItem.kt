package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class BottomTabBarItem(
    val label: String,
    // a short mark over the icon, e.g. the round of a fight in progress
    val badge: String? = null,
    // a raised item is drawn as a round button over the bar, it belongs in the middle slot
    val raised: Boolean = false,
    val contentDescription: String = label,
    val icon: @Composable (tint: Color) -> Unit,
)
