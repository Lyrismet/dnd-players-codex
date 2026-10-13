package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmap
import com.lyrismet.incadent.domain.model.Photo

/** desaturates a monochrome portrait - a dead npc's photo, matching the mockup's grayscale(1) filter */
val PortraitGrayscaleFilter: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * read-only avatar slot - a decoded [portrait] thumbnail cropped to fill when present, otherwise [emblem] (the
 * initial letter or glyph every entity already falls back to). Shared by the codex list row (44dp) and the
 * entity sheet header (58dp) so the decode-and-render branch exists exactly once.
 */
@Composable
fun PortraitBadge(
    portrait: Photo?,
    color: StatusColor,
    size: Dp,
    shape: Shape = CircleShape,
    monochrome: Boolean = false,
    modifier: Modifier = Modifier,
    emblem: @Composable () -> Unit,
) {
    IconBadge(modifier = modifier, size = size, shape = shape, border = color.border, borderWidth = 1.5.dp) {
        if (portrait != null) {
            // a badge is small, so it draws the thumbnail and never decodes the full picture
            val bitmap = rememberPhotoBitmap(portrait.thumb)
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (monochrome) PortraitGrayscaleFilter else null,
            )
        } else {
            emblem()
        }
    }
}
