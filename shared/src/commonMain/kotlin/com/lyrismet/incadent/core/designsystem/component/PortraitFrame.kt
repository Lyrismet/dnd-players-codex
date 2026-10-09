package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.portrait.decodePortraitBitmap

private val PortraitFrameWidth = 72.dp
private val PortraitFrameHeight = 94.dp
private val PortraitFrameShape =
    RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

// mockup's diamond ornament is a square rotated to stand on one corner
private const val DIAMOND_ROTATION_DEGREES = 45f
private val DiamondSize = 6.dp

/**
 * the arch-shaped portrait slot of the create/edit form, see Players Codex v6.dc.html's form `photo()` - a decoded
 * [portraitBase64] cropped to fill when present, otherwise a gold "+". The ring turns gold once a portrait is set.
 */
@Composable
fun PortraitFrame(
    portraitBase64: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(width = PortraitFrameWidth, height = PortraitFrameHeight)) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(PortraitFrameShape)
                    .background(AppPalette.Surface)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (portraitBase64 != null) {
                val bitmap = remember(portraitBase64) { decodePortraitBitmap(portraitBase64) }
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text("+", color = AppPalette.GoldBright, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
            }
            val ring = if (portraitBase64 != null) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.BorderHover
            Box(Modifier.matchParentSize().border(1.5.dp, ring, PortraitFrameShape))
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -(DiamondSize / 2))
                    .size(DiamondSize)
                    .rotate(DIAMOND_ROTATION_DEGREES)
                    .background(AppPalette.Gold),
        )
    }
}
