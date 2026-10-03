package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class BottomTabBarItem(
    val label: String,
    val icon: @Composable (tint: Color) -> Unit,
)
