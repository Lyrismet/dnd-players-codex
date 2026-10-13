package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.core.portrait.CropAspect
import com.lyrismet.incadent.core.portrait.CropGeometry
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.CropState
import com.lyrismet.incadent.core.portrait.MAX_ZOOM
import com.lyrismet.incadent.core.portrait.MIN_ZOOM
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.photo_crop_cancel
import dndplayerscodex.shared.generated.resources.photo_crop_done
import dndplayerscodex.shared.generated.resources.photo_crop_hint
import dndplayerscodex.shared.generated.resources.photo_crop_overline
import dndplayerscodex.shared.generated.resources.photo_crop_reset
import org.jetbrains.compose.resources.stringResource

private val PortraitWindowShape =
    RoundedCornerShape(topStart = 120.dp, topEnd = 120.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
private val WideWindowShape = RoundedCornerShape(14.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

// the mockup's crop screen - #0B0C10 ground, the picture outside the frame at 28%, a 35% shade and a thirds grid
private val CropBackground = Color(0xFF0B0C10)
private val OutsideShade = Color(0x590B0C10)
private val GridLine = Color(0x47F1EBDD)
private const val OUTSIDE_IMAGE_ALPHA = 0.28f
private const val THIRD = 1f / 3f
private const val ZOOM_STEP = 0.25f
private const val TENTHS = 10
private const val SHADE_REACH_PX = 10_000f

private fun CropAspect.windowShape(): Shape = if (this == CropAspect.Portrait) PortraitWindowShape else WideWindowShape

/** the full-screen crop window, "Готово" reports the chosen [CropRect] and nothing is cut here */
@Composable
fun AppPhotoCropper(
    source: ImageBitmap,
    aspect: CropAspect,
    title: String,
    onConfirm: (CropRect) -> Unit,
    onCancel: () -> Unit,
) {
    // a window floats above everything, so it goes with its tab like [AppDialog]
    if (!LocalTabSelected.current) return
    val geometry = remember(aspect, source) { CropGeometry(aspect, source.width, source.height) }
    var state by remember(geometry) { mutableStateOf(geometry.initial()) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CropBackground)
                    .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            CropHeader(title = title, onCancel = onCancel, onDone = { onConfirm(geometry.toCropRect(state)) })
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                CropWindow(source, aspect, geometry, state) { state = it }
            }
            CropControls(
                zoom = state.zoom,
                onZoom = { state = geometry.zoomTo(state, it) },
                onReset = { state = geometry.initial() },
            )
        }
    }
}

@Composable
private fun CropHeader(
    title: String,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.height(44.dp).clickable(onClick = onCancel).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.photo_crop_cancel),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppPalette.TextMuted,
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.photo_crop_overline),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.16.em,
                color = AppPalette.Gold,
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp, lineHeight = 24.sp),
                color = AppPalette.TextHeading,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier =
                Modifier
                    .height(44.dp)
                    .background(AppPalette.Gold, ButtonShape)
                    .clickable(onClick = onDone)
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.photo_crop_done),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppPalette.Background,
            )
        }
    }
}

@Composable
private fun CropWindow(
    source: ImageBitmap,
    aspect: CropAspect,
    geometry: CropGeometry,
    state: CropState,
    onStateChange: (CropState) -> Unit,
) {
    val density = LocalDensity.current.density
    val shape = aspect.windowShape()
    // the drag lambda lives across the whole gesture, so it must read the newest state, not the one it started with
    val latestState by rememberUpdatedState(state)
    val latestOnChange by rememberUpdatedState(onStateChange)
    Box(
        modifier =
            Modifier
                .requiredSize(aspect.viewWidth.dp, aspect.viewHeight.dp)
                .pointerInput(geometry) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        latestOnChange(geometry.dragBy(latestState, drag.x / density, drag.y / density))
                    }
                }.drawWithContent {
                    drawCropLayers(source, shape, geometry, state)
                },
    )
}

// layers as in the mockup - the whole picture dim, the framed part bright with a grid, a shade around, a gold edge
private fun DrawScope.drawCropLayers(
    source: ImageBitmap,
    shape: Shape,
    geometry: CropGeometry,
    state: CropState,
) {
    val topLeft = IntOffset((state.offsetX * density).toInt(), (state.offsetY * density).toInt())
    val displaySize =
        IntSize(
            (geometry.displayWidth(state) * density).toInt().coerceAtLeast(1),
            (geometry.displayHeight(state) * density).toInt().coerceAtLeast(1),
        )
    val window = shape.createOutline(size, layoutDirection, this).toPath()
    drawImage(
        source,
        dstOffset = topLeft,
        dstSize = displaySize,
        alpha = OUTSIDE_IMAGE_ALPHA,
        filterQuality = FilterQuality.Medium,
    )
    clipPath(window) {
        drawImage(source, dstOffset = topLeft, dstSize = displaySize, filterQuality = FilterQuality.Medium)
        drawThirdsGrid()
    }
    clipPath(window, ClipOp.Difference) {
        drawRect(
            OutsideShade,
            topLeft = Offset(-SHADE_REACH_PX, -SHADE_REACH_PX),
            size = Size(size.width + 2 * SHADE_REACH_PX, size.height + 2 * SHADE_REACH_PX),
        )
    }
    drawPath(window, AppPalette.Gold, style = Stroke(width = 2.dp.toPx()))
}

private fun Outline.toPath(): Path =
    when (this) {
        is Outline.Rectangle -> Path().apply { addRect(rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
        is Outline.Generic -> path
    }

private fun DrawScope.drawThirdsGrid() {
    val line = 1.dp.toPx()
    listOf(THIRD, 2 * THIRD).forEach { fraction ->
        val x = size.width * fraction
        drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = line)
        val y = size.height * fraction
        drawLine(GridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = line)
    }
}

@Composable
private fun CropControls(
    zoom: Float,
    onZoom: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZoomButton("−") { onZoom((zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)) }
            Slider(
                value = zoom,
                onValueChange = onZoom,
                valueRange = MIN_ZOOM..MAX_ZOOM,
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = AppPalette.Gold,
                        activeTrackColor = AppPalette.Gold,
                        inactiveTrackColor = AppPalette.BorderHover,
                    ),
            )
            ZoomButton("+") { onZoom((zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tenths = (zoom * TENTHS).toInt()
            Text(
                stringResource(Res.string.photo_crop_hint, "${tenths / TENTHS}.${tenths % TENTHS}×"),
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                lineHeight = 17.4.sp,
                color = AppPalette.TextSecondary,
            )
            Box(
                modifier =
                    Modifier
                        .height(36.dp)
                        .clickable(onClick = onReset)
                        .border(1.dp, AppPalette.BorderHover, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.photo_crop_reset),
                    fontSize = 12.5.sp,
                    color = AppPalette.TextDescription,
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    glyph: String,
    onClick: () -> Unit,
) {
    IconBadge(
        size = 44.dp,
        shape = ButtonShape,
        background = AppPalette.Surface,
        border = AppPalette.Border,
        onClick = onClick,
    ) { Text(glyph, color = AppPalette.TextPrimary, fontSize = 20.sp) }
}
