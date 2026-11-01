package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val ICON_VIEWPORT = 24f

// the first path cuts its details out (even-odd), the optional second one is a separate filled shape
fun silhouetteIcon(
    name: String,
    body: String,
    extra: String = "",
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
        ).addPath(
            pathData = addPathNodes(body),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        ).apply {
            if (extra.isNotEmpty()) addPath(pathData = addPathNodes(extra), fill = SolidColor(Color.Black))
        }.build()
