package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

/** the small caption of a sheet section with its help hint */
@Composable
internal fun MeSectionTitle(
    title: String,
    helpTitle: String,
    helpBody: String,
    modifier: Modifier = Modifier,
    hintModifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = AppText.fieldOverline, color = AppPalette.TextTertiary)
        AppHelpHint(helpTitle, helpBody, hintModifier)
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
