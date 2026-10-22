package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** a 17dp parchment line icon from SVG [paths] on the design's 24 grid, 1.6 strokes with round ends */
fun parchmentStrokeIcon(
    name: String,
    vararg paths: String,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 17.dp, defaultHeight = 17.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            paths.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(AppPalette.Parchment),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

/** the box-with-arrow-up that says "goes out to a file" */
val ExportIcon: ImageVector = parchmentStrokeIcon("Export", "M12 15V4", "M7.5 8.5 12 4l4.5 4.5", "M5 14v5h14v-5")

/** the box-with-arrow-down that says "comes in from a file" */
val ImportIcon: ImageVector = parchmentStrokeIcon("Import", "M12 4v11", "M7.5 10.5 12 15l4.5-4.5", "M5 14v5h14v-5")

/** a settings row with a 32dp icon tile in front - title 20sp and subtitle 16sp tall, the row the mockup repeats */
@Composable
fun AppIconListRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
) {
    AppListRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        value = value,
        onClick = onClick,
        minHeight = 56.dp,
        titleLineHeight = 20.sp,
        subtitleLineHeight = 16.sp,
        leading = {
            IconBadge(
                size = 32.dp,
                shape = AppShapes.Tile,
                background = AppPalette.SurfacePopover,
                border = AppPalette.Border,
            ) {
                Image(imageVector = icon, contentDescription = null)
            }
        },
    )
}
