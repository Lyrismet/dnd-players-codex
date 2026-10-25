package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.format.NumberSizeLadder

/** centered fixed-width index (session number, note number) in front of a list row, shrinking as the label grows */
@Composable
fun NumberLabel(
    text: String,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
    color: Color = AppPalette.Gold,
    sizeLadder: NumberSizeLadder = NumberSizeLadder.ARCHIVE_SESSION,
    lineHeightFactor: Float = 1.35f,
) {
    val fontSize = sizeLadder.sizeFor(text).sp
    val base = MaterialTheme.typography.titleMedium
    val style =
        remember(base, fontSize, lineHeightFactor) {
            base.copy(
                fontSize = fontSize,
                lineHeight =
                    fontSize * lineHeightFactor,
            )
        }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.width(width),
    )
}
