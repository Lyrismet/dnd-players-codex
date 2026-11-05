package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint

private val CAPTION_HEIGHT = 12.dp
private val HINT_SIZE = 28.dp

/** the small caption of a section with its help hint when there is one */
@Composable
internal fun MeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    helpTitle: String? = null,
    helpBody: String? = null,
) {
    Row(modifier.height(CAPTION_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = AppText.fieldOverline, color = AppPalette.TextTertiary)
        if (helpTitle != null && helpBody != null) AppHelpHint(helpTitle, helpBody, Modifier.requiredSize(HINT_SIZE))
    }
}

/** the centered title and hint of a tab with nothing in it yet, the actions come as the content */
@Composable
internal fun MeEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
        Text(body, style = AppText.footnote, color = AppPalette.TextSecondary, textAlign = TextAlign.Center)
        content()
    }
}
