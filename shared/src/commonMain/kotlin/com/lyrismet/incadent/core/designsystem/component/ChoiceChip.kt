package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** 30dp pill that picks one value out of a row - the selected one fills gold */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) AppPalette.Gold else AppPalette.Surface
    val foreground = if (selected) AppPalette.Background else AppPalette.TextDescription
    val border = if (selected) AppPalette.Gold else AppPalette.Border
    Row(
        modifier =
            modifier
                .height(30.dp)
                .appCard(
                    shape = RoundedCornerShape(15.dp),
                    background = background,
                    border = border,
                    onClick = onClick,
                ).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = foreground,
        )
    }
}
