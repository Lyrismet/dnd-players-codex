package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.centeredLine
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.sqrt

private val CenterSize = 48.dp
private val CenterOverlap = 7.dp
private val NotchGap = 6.dp
private val NotchRadius = CenterSize / 2 + NotchGap
private val NotchCenterY = CenterSize / 2 - CenterOverlap
private val BarTopPadding = 8.dp

/**
 * persistent bottom navigation bar - gold top indicator on the active tab, muted icon+label otherwise. A
 * [BottomTabBarItem.raised] item is drawn as a round button raised [CenterOverlap] over the bar, the bar is cut out
 * around it
 */
@Composable
fun BottomTabBar(
    tabs: List<BottomTabBarItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val raisedIndex = tabs.indexOfFirst { it.raised }
    val raised = tabs.getOrNull(raisedIndex)
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (raised != null) Spacer(Modifier.height(CenterOverlap))
            Column(modifier = Modifier.fillMaxWidth().barBackground(raised != null)) {
                TabsRow(tabs, selectedIndex, onTabSelected)
            }
        }
        if (raised != null) {
            CenterButton(
                item = raised,
                selected = selectedIndex == raisedIndex,
                onClick = { onTabSelected(raisedIndex) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

// the bar, its top line, and when there is a raised button a round hole around it that shows what is behind
private fun Modifier.barBackground(hasNotch: Boolean): Modifier =
    drawWithCache {
        val line = 1.dp.toPx()
        val radius = NotchRadius.toPx()
        val centerX = size.width / 2
        val centerY = NotchCenterY.toPx()
        val edge = asin((centerY / radius).coerceIn(-1f, 1f)).let { it * RADIANS_TO_DEGREES }
        val half = sqrt(radius * radius - centerY * centerY)
        val bar = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
        val hole = Path().apply { addOval(Rect(Offset(centerX, centerY), radius)) }
        val shape = if (hasNotch) Path.combine(PathOperation.Difference, bar, hole) else bar
        onDrawBehind {
            drawPath(shape, AppPalette.NavBar)
            if (!hasNotch) {
                drawLine(AppPalette.BorderSubtle, Offset(0f, line / 2), Offset(size.width, line / 2), line)
                return@onDrawBehind
            }
            drawLine(AppPalette.BorderSubtle, Offset(0f, line / 2), Offset(centerX - half, line / 2), line)
            drawLine(AppPalette.BorderSubtle, Offset(centerX + half, line / 2), Offset(size.width, line / 2), line)
            drawArc(
                color = AppPalette.BorderSubtle,
                startAngle = -edge,
                sweepAngle = 180f + 2 * edge,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(line),
            )
        }
    }

private const val RADIANS_TO_DEGREES = (180.0 / PI).toFloat()

@Composable
private fun TabsRow(
    tabs: List<BottomTabBarItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    // the labels share one baseline, so the item that holds the round button decides the height
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(top = BarTopPadding, start = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val tint = if (selected) AppPalette.GoldBright else AppPalette.TextTertiary
            val isCenter = tab.raised
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTabSelected(index) }) {
                if (!isCenter) SelectedIndicator(selected, Modifier.align(Alignment.TopCenter))
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
                ) {
                    if (isCenter) {
                        Spacer(Modifier.height(CenterIconSpace))
                    } else {
                        Box {
                            tab.icon(tint)
                            tab.badge?.let {
                                TabBadge(it, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-4).dp))
                            }
                        }
                    }
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

// the round button covers the icon row and leaves room for the label under it
private val CenterIconSpace = 27.dp

// sits on the bar's top border line, 9dp above the button like the design's `top: -9px`
@Composable
private fun SelectedIndicator(
    selected: Boolean,
    modifier: Modifier,
) {
    Spacer(
        modifier
            .offset(y = (-9).dp)
            .width(22.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(if (selected) AppPalette.GoldBright else Color.Transparent),
    )
}

@Composable
private fun CenterButton(
    item: BottomTabBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glyph = if (selected) AppPalette.Background else AppPalette.GoldBright
    Box(
        modifier =
            modifier
                .size(CenterSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(if (selected) AppPalette.Gold else AppPalette.SurfacePopover)
                .clickable(onClickLabel = item.contentDescription, onClick = onClick)
                .semantics { contentDescription = item.contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        item.icon(glyph)
    }
}

@Composable
private fun TabBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppPalette.Maroon)
                .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.centeredLine(),
            color = AppPalette.TextHeading,
        )
    }
}
