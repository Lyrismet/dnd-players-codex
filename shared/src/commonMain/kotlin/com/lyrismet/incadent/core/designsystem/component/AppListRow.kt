package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** the "›" that closes a row opening something - one glyph and size everywhere */
@Composable
fun AppChevron(modifier: Modifier = Modifier) {
    Text("›", fontSize = AppFontSize.Heading, color = AppPalette.TextTertiary, modifier = modifier)
}

/**
 * the card a group of [AppListRow]s sits in - put an [AppDivider] between the rows. [contentPadding] is the inset
 * of the rows from the card's edge.
 */
@Composable
fun AppListCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Surface)
                .padding(contentPadding),
        content = content,
    )
}

/**
 * one row of a settings-like list: [title] with a [subtitle] under it, a [leading] slot before, a [value] and a
 * [trailing] slot after, and a chevron when it opens something ([onClick] set). Pass [onClick] only for a row that
 * is itself the target, a row with its own control (a button, a switch) keeps the tap to that control.
 */
@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = AppPalette.GoldBright,
    valueWeight: FontWeight = FontWeight.SemiBold,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = onClick != null,
    minHeight: Dp = if (onClick != null) 52.dp else 0.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = if (onClick != null) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = AppFontSize.Medium,
                fontWeight = FontWeight.Medium,
                color = AppPalette.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = AppFontSize.Small,
                    color = AppPalette.TextSecondary,
                )
            }
        }
        if (value != null) {
            Text(text = value, fontSize = AppFontSize.Body, fontWeight = valueWeight, color = valueColor)
        }
        trailing?.invoke(this)
        if (chevron) AppChevron()
    }
}
