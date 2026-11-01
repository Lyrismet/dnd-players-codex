package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon

/** what a portrait badge or arch shows while it has no photo - an initial letter or an entity icon */
sealed interface PortraitMark {
    data class Letter(
        val text: String,
    ) : PortraitMark

    data class Icon(
        val icon: ImageVector,
    ) : PortraitMark
}

/** draws [mark] in [tint], the letter in [letterStyle] and the icon in a square of [iconSize] */
@Composable
fun PortraitMarkContent(
    mark: PortraitMark,
    tint: Color,
    letterStyle: TextStyle,
    iconSize: Dp = 24.dp,
) {
    when (mark) {
        is PortraitMark.Icon -> AppEntityIcon(mark.icon, tint, size = iconSize)
        is PortraitMark.Letter -> Text(mark.text, style = letterStyle, color = tint)
    }
}
