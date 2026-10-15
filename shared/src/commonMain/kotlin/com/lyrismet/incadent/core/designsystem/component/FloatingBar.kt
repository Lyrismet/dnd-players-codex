package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private val FloatingBarShape = RoundedCornerShape(14.dp)
private val FloatingBarPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)

/** keeps the last non-null [value] while it is null, so a bar that fades out still shows what it held */
@Composable
fun <T : Any> rememberLingering(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != null) last = value }
    return value ?: last
}

/** pins a floating bar to the bottom center of a Box, [bottomInset] above its edge */
fun BoxScope.floatingBarAnchor(bottomInset: Dp = 12.dp): Modifier =
    Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = bottomInset)

/**
 * the raised card toasts, hints and the undo bar share: it slides and fades in and out, drawn on the elevated
 * surface with a [border] (or a drop shadow when [shadowed], for a bar over busy content). Keep it composed and
 * toggle [visible].
 */
@Composable
fun FloatingBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    border: Color? = AppPalette.BorderHover,
    shadowed: Boolean = false,
    contentPadding: PaddingValues = FloatingBarPadding,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .then(if (shadowed) Modifier.shadow(elevation = 10.dp, shape = FloatingBarShape) else Modifier)
                    .appCard(FloatingBarShape, AppPalette.SurfaceElevated, if (shadowed) null else border)
                    .padding(contentPadding),
        ) {
            content()
        }
    }
}

/** a [FloatingBar] driven by what it shows - visible while [value] is set, the last one lingers as it fades out */
@Composable
fun <T : Any> FloatingBar(
    value: T?,
    modifier: Modifier = Modifier,
    border: Color? = AppPalette.BorderHover,
    shadowed: Boolean = false,
    contentPadding: PaddingValues = FloatingBarPadding,
    content: @Composable (T) -> Unit,
) {
    val shown = rememberLingering(value)
    FloatingBar(
        visible = value != null,
        modifier = modifier,
        border = border,
        shadowed = shadowed,
        contentPadding = contentPadding,
    ) {
        shown?.let { content(it) }
    }
}
