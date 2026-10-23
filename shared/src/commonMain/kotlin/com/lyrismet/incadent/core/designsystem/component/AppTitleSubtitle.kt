package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText

/**
 * a name with the line under it, both on the design's 20 line (`AppText.rowTitle` and `AppText.rowSubtitle`) - the
 * text block of a list row, a sheet row or a step. The subtitle is left out when it is blank.
 */
@Composable
fun AppTitleSubtitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = AppPalette.TextPrimary,
    subtitleColor: Color = AppPalette.TextSecondary,
    maxLines: Int = 1,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppText.rowTitle,
            color = titleColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = AppText.rowSubtitle,
                color = subtitleColor,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
