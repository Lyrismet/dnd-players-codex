package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

private const val DIAMOND_ROTATION_DEGREES = 45f

/** centered gold diamond + serif title + hint - the "blank page" state of a screen that is meant to be filled */
@Composable
fun OrnamentedEmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(10.dp).rotate(DIAMOND_ROTATION_DEGREES).border(1.dp, AppPalette.Gold))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                color = AppPalette.TextHeading,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.7.sp),
                color = AppPalette.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}
