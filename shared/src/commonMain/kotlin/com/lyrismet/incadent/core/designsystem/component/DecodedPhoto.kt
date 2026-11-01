package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmapAsync
import com.lyrismet.incadent.domain.model.Photo

/** the photo cropped to fill its box, a loader in the middle while it decodes */
@Composable
internal fun BoxScope.DecodedPhoto(
    photo: Photo,
    monochrome: Boolean = false,
) {
    val bitmap = rememberPhotoBitmapAsync(photo.display)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = if (monochrome) PortraitGrayscaleFilter else null,
        )
    } else {
        AppLoader(size = PhotoLoaderSize, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center))
    }
}
