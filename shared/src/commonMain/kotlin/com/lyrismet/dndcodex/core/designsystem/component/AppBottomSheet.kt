package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val SheetScrimColor = Color(0xFF050508).copy(alpha = 0.66f)
private val SheetEdgeColor = AppPalette.Gold.copy(alpha = 0.4f)
private val SheetCornerRadius = 26.dp

/** modal bottom sheet matching the design's entity/action sheets - 26dp top corners, dark surface, thin gold edge */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // no PartiallyExpanded anchor - otherwise it opens to a ~50% anchor first and needs a drag
        // to reach full content height, even when the content would already fit
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        shape = SheetShape,
        containerColor = AppPalette.Surface,
        scrimColor = SheetScrimColor,
        dragHandle = { AppBottomSheetDragHandle() },
        content = { content() },
    )
}

// Material3's Surface discards an external Modifier.border(), so the gold edge is hand-drawn here instead
@Composable
private fun AppBottomSheetDragHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SheetCornerRadius + 4.dp)
                .drawWithContent {
                    drawContent()
                    val radiusPx = SheetCornerRadius.toPx()
                    val path =
                        Path().apply {
                            moveTo(0f, radiusPx)
                            arcTo(Rect(0f, 0f, radiusPx * 2, radiusPx * 2), 180f, 90f, false)
                            lineTo(size.width - radiusPx, 0f)
                            arcTo(Rect(size.width - radiusPx * 2, 0f, size.width, radiusPx * 2), 270f, 90f, false)
                        }
                    drawPath(path, color = SheetEdgeColor, style = Stroke(width = 1.5.dp.toPx()))
                },
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.BorderHover),
        )
    }
}

// raw "✕" glyph, not a Material icon - matches this codebase's existing raw-glyph precedent (MentionGlyph's symbols)
@Composable
fun SheetCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    IconBadge(modifier = modifier, size = size, background = AppPalette.BorderSubtle, onClick = onClick) {
        Text("✕", fontSize = 13.sp, color = AppPalette.TextMuted)
    }
}
