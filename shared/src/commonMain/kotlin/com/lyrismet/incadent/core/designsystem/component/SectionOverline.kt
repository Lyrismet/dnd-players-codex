package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the tracked all-caps label used for page eyebrows and list section headers (e.g. "АРХИВ", "БАЗА ЗНАНИЙ") */
@Composable
fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.Gold,
    letterSpacing: TextUnit = 0.18.em,
    fontSize: TextUnit = TextUnit.Unspecified,
    // tapping the text and [inlineContent] together, e.g. an editable title with a pen after it
    onClick: (() -> Unit)? = null,
    inlineContent: (@Composable () -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val baseStyle = MaterialTheme.typography.labelSmall
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val tappable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Row(modifier = Modifier.weight(1f).then(tappable), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text.uppercase(),
                style =
                    baseStyle.copy(
                        fontSize = if (fontSize.isSpecified) fontSize else baseStyle.fontSize,
                        letterSpacing = letterSpacing,
                    ),
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
