package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** one tab of [AppSegmentedTabs], [count] is shown dimmed after the label when set */
data class SegmentedTab<T>(
    val value: T,
    val label: String,
    val count: Int? = null,
    val icon: ImageVector? = null,
    // tints the label, full when selected and dimmed otherwise, instead of the usual gold and gray
    val accent: Color? = null,
)
