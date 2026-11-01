package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon

/** the tracked all-caps label used for page eyebrows and list section headers (e.g. "АРХИВ", "БАЗА ЗНАНИЙ") */
@Composable
fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.Gold,
    style: OverlineStyle = OverlineStyle.Page,
    // a value here replaces what [style] sets
    letterSpacing: TextUnit = style.letterSpacing,
    fontSize: TextUnit = style.fontSize,
    // tapping the text and [inlineContent] together, e.g. an editable title with a pen after it
    onClick: (() -> Unit)? = null,
    inlineContent: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val baseStyle = MaterialTheme.typography.labelSmall
    val upperText = remember(text) { text.uppercase() }
    val textStyle =
        remember(baseStyle, fontSize, letterSpacing) {
            baseStyle.copy(
                fontSize = if (fontSize.isSpecified) fontSize else baseStyle.fontSize,
                letterSpacing = letterSpacing,
            )
        }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val tappable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Row(modifier = Modifier.weight(1f).then(tappable), verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                AppEntityIcon(leadingIcon, color, Modifier.padding(end = 6.dp), size = 12.dp)
            }
            Text(
                text = upperText,
                style = textStyle,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            inlineContent?.invoke()
        }
        trailingContent()
    }
}
