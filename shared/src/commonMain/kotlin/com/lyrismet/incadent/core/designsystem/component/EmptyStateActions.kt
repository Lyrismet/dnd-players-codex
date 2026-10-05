package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                color = AppPalette.TextHeading,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.7.sp),
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
    Button(
        onClick = action.onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Gold, contentColor = AppPalette.Background),
        modifier = Modifier.padding(top = 4.dp).height(48.dp).widthIn(max = 280.dp),
    ) {
        Text(
            action.label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SecondaryEmptyStateButton(action: EmptyStateAction) {
    OutlinedButton(
        onClick = action.onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppPalette.BorderHover),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.TextDescription),
        modifier = Modifier.height(40.dp).widthIn(max = 280.dp),
    ) {
        Text(
            action.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
