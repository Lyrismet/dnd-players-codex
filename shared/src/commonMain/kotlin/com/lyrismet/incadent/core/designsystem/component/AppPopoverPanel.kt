package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

// tall enough for the seven rows and three headers of an empty "@", anything more scrolls
private val PopoverMaxHeight = 360.dp
private const val POPOVER_MAX_WINDOW_SHARE = 0.4f

/**
 * the floating list card of the mockup - popover surface, border and shadow, a scrolling [content] and an optional
 * [footer] that stays in view under it. On a short screen with the keyboard up it takes a share of the window.
 */
@Composable
fun AppPopoverPanel(
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = AppShapes.Card
    val windowShare =
        with(LocalDensity.current) {
            (
                LocalWindowInfo.current.containerSize.height *
                    POPOVER_MAX_WINDOW_SHARE
            ).toDp()
        }
    Column(
        modifier =
            modifier
                .shadow(elevation = 16.dp, shape = shape)
                .appCard(shape = shape, background = AppPalette.SurfacePopover, border = AppPalette.BorderPopover)
                .heightIn(max = minOf(PopoverMaxHeight, windowShare)),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
        if (footer != null) {
            AppDivider(color = AppPalette.BorderPopover)
            Column(modifier = Modifier.padding(6.dp)) { footer() }
        }
    }
}
