package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** a radio-style option row with a title and a description - the selected one is gold-tinted and ringed */
@Composable
fun RadioOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = if (selected) AppPalette.Gold.copy(alpha = 0.07f) else AppPalette.Background,
                    border = if (selected) AppPalette.Gold.copy(alpha = 0.5f) else AppPalette.BorderSubtle,
                    onClick = onClick,
                ).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioDot(selected, Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.TextHeading,
            )
            Text(
                description,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = AppFontSize.SmallPlus,
                        lineHeight = 18.75.sp,
                    ),
                color = AppPalette.TextMuted,
            )
        }
    }
}

/** the 18dp radio ring with a gold dot when selected */
@Composable
internal fun RadioDot(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val ring = if (selected) AppPalette.Gold else AppPalette.RadioIdle
    Box(
        modifier = modifier.size(18.dp).border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(8.dp).background(if (selected) AppPalette.Gold else Color.Transparent, CircleShape))
    }
}
