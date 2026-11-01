package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.domain.model.Photo

private val PortraitFrameWidth = 72.dp
private val PortraitFrameHeight = 94.dp
private val PortraitFrameShape =
    RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

// mockup's diamond ornament is a square rotated to stand on one corner
private val DiamondSize = 6.dp

/**
 * the arch-shaped portrait slot of the create/edit form, see Players Codex v6.dc.html's form `photo()` - a decoded
 * [portrait] cropped to fill when present, otherwise a gold "+". The ring turns gold once a portrait is set.
 */
@Composable
fun PortraitFrame(
    portrait: Photo?,
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
            if (portrait != null) {
                DecodedPhoto(portrait)
            } else {
                Text(
                    "+",
                    color = AppPalette.GoldBright,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = AppFontSize.SubDisplay,
                )
            }
            val ring = if (portrait != null) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.BorderHover
            Box(Modifier.matchParentSize().border(1.5.dp, ring, PortraitFrameShape))
        }
        DiamondFill(
            size = DiamondSize,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = -(DiamondSize / 2)),
        )
    }
}
