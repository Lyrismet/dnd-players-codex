package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

private const val CLOCK_MILLIS = 4000

// the app theme on the app background, so a preview looks like the screen it will sit on
@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    AppTheme {
        Column(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Preview
@Composable
private fun AppCellsPreview() {
    PreviewSurface {
        CellSize.entries.forEach { size ->
            AppCells(total = 5, filled = 3, size = size, tone = CellTone.Arcane)
            AppCells(total = 5, filled = 3, size = size, tone = CellTone.Gold)
        }
    }
}

@Preview
@Composable
private fun AppCellsTappablePreview() {
    PreviewSurface {
        var filled by remember { mutableStateOf(2) }
        AppCells(total = 4, filled = filled, size = CellSize.Big, onPress = { filled = it + 1 })
    }
}

@Preview
@Composable
private fun CellEffectPreview() {
    PreviewSurface {
        val transition = rememberInfiniteTransition()
        val clock =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(CLOCK_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            )
        CellEffect.entries.forEach { effect ->
            AppCells(total = 3, filled = 3, size = CellSize.Big, effect = effect, clock = clock)
        }
    }
}

@Preview
@Composable
private fun AppToggleCardPreview() {
    PreviewSurface {
        var checked by remember { mutableStateOf(true) }
        val toggle = { checked = !checked }
        AppToggleCard(title = "Concentration", hint = "Only one spell at a time", checked = checked, onToggle = toggle)
        AppToggleCard(title = "Ritual", hint = null, checked = false, onToggle = {})
    }
}
