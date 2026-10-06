package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.QuillIcon
import kotlinx.coroutines.delay

private const val HINT_VISIBLE_MS = 2000L

// the mockup draws the pin at 1.2em of a 17px glyph
private const val PIN_SIZE_DP = 20.4f

/** the one-time row that teaches holding a field - sits under the card header, dismissed with its own button */
@Composable
fun HoldHintCard(
    text: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .offset(y = (-4).dp)
                .appCard(
                    shape = RoundedCornerShape(12.dp),
                    background = AppPalette.Gold.copy(alpha = 0.08f),
                    border = AppPalette.Gold.copy(alpha = 0.4f),
                ).padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuillIcon(size = PIN_SIZE_DP.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.5.sp),
            color = AppPalette.TextHeading,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Text(
                dismissLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = AppPalette.GoldBright,
            )
        }
    }
}

/** a nudge that fades in for two seconds each time [trigger] changes - bumped on every too-short press */
@Composable
fun HoldTooShortToast(
    trigger: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(HINT_VISIBLE_MS)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = AppPalette.TextPrimary,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppPalette.SurfaceElevated)
                    .border(1.dp, AppPalette.BorderHover, RoundedCornerShape(14.dp))
                    .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        )
    }
}
