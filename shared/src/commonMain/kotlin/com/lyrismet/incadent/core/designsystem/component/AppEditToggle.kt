package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons

/**
 * the pen and the word "Edit" with no frame, the way into an edit mode (the queue of a fight, the session list).
 * [compact] is the small one for the end of a section caption, [withPen] false leaves the pen out (a "+ Add" word).
 */
@Composable
fun AppEditButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    withPen: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withPen) {
            Icon(
                AppIcons.Pencil,
                contentDescription = null,
                tint = AppPalette.Gold,
                modifier = Modifier.size(if (compact) 12.dp else 16.dp),
            )
        }
        Text(
            text = text,
            color = AppPalette.Gold,
            fontSize = if (compact) AppFontSize.Small else AppFontSize.MediumPlus,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** the gold "Done" box that leaves an edit mode */
@Composable
fun AppDoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(44.dp)
                .background(AppPalette.Gold, RoundedCornerShape(11.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = AppPalette.Background,
            fontSize = AppFontSize.Medium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
