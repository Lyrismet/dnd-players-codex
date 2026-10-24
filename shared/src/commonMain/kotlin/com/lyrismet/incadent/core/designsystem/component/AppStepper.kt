package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

private const val DISABLED_ALPHA = 0.25f

/**
 * the "- 3 +" count control: the minus turns into a cross when one more press removes the last one, the plus dims
 * once [canIncrease] is off. Each button is a 44dp target.
 */
@Composable
fun AppStepper(
    count: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    canIncrease: Boolean = true,
) {
    Row(
        modifier = modifier.clip(AppShapes.Field).border(1.dp, AppPalette.BorderHover, AppShapes.Field),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            glyph = if (count <= 1) "✕" else "−",
            color = AppPalette.TextMuted,
            description = decreaseDescription,
            onClick = onDecrease,
        )
        Text(
            text = count.toString(),
            modifier = Modifier.widthIn(min = 26.dp),
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.MediumPlus,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        StepperButton(
            glyph = "+",
            color = AppPalette.GoldBright,
            description = increaseDescription,
            onClick = onIncrease,
            enabled = canIncrease,
        )
    }
}

@Composable
private fun StepperButton(
    glyph: String,
    color: Color,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = color, fontSize = AppFontSize.HeadingPlus)
    }
}
