package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette

/** centered serif title, hint and up to two buttons - the empty state of a list the user is meant to fill */
@Composable
fun EmptyStateActions(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    primaryAction: EmptyStateAction? = null,
    secondaryAction: EmptyStateAction? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = AppFontSize.HeadingPlus),
                color = AppPalette.TextHeading,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = AppFontSize.Medium, lineHeight = 21.7.sp),
                color = AppPalette.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            primaryAction?.let { PrimaryEmptyStateButton(it) }
            secondaryAction?.let { SecondaryEmptyStateButton(it) }
        }
    }
}

@Composable
private fun PrimaryEmptyStateButton(action: EmptyStateAction) {
    AppButton(
        text = action.label,
        onClick = action.onClick,
        modifier = Modifier.padding(top = 4.dp).widthIn(max = 280.dp),
        variant = ButtonVariant.Solid,
        size = ButtonSize.Primary,
        height = 48.dp,
    )
}

@Composable
private fun SecondaryEmptyStateButton(action: EmptyStateAction) {
    AppButton(
        text = action.label,
        onClick = action.onClick,
        modifier = Modifier.widthIn(max = 280.dp),
        tone = ButtonTone.Neutral,
        size = ButtonSize.Large,
        height = 40.dp,
    )
}
