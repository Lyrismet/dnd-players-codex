package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.portrait.decodePortraitBitmap

private val PortraitArchShape =
    RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
private val PortraitArchInsetShape =
    RoundedCornerShape(topStart = 39.dp, topEnd = 39.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

// mockup's diamond is a square rotated to stand on one corner, with a 3px halo in the sheet colour
private const val DIAMOND_ROTATION_DEGREES = 45f
private val DiamondSize = 8.dp
private val DiamondHaloSize = 14.dp

/**
 * the npc/party portrait of the entity sheet header, see Players Codex v6.dc.html's sheet `sh.isPortrait` - an 88x114
 * arch with a gold inset ring and diamond. A decoded [portraitBase64] fills it, otherwise [emblemText]. [onClick]
 * non-null makes it tappable and, while empty, adds the "+" badge that invites a tap.
 */
@Composable
fun PortraitArch(
    portraitBase64: String?,
    emblemText: String,
    color: StatusColor,
    monochrome: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.size(width = 88.dp, height = 114.dp)) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(PortraitArchShape)
                    .background(AppPalette.Background)
                    .border(1.5.dp, color.border, PortraitArchShape)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (portraitBase64 != null) {
                val bitmap = remember(portraitBase64) { decodePortraitBitmap(portraitBase64) }
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (monochrome) PortraitGrayscaleFilter else null,
                )
            } else {
                Text(
                    emblemText,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    color = color.foreground,
                )
            }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(5.dp)
                        .border(1.dp, AppPalette.Gold.copy(alpha = 0.4f), PortraitArchInsetShape),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -(DiamondHaloSize / 2))
                    .size(DiamondHaloSize)
                    .rotate(DIAMOND_ROTATION_DEGREES)
                    .background(AppPalette.Surface),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -(DiamondSize / 2))
                    .size(DiamondSize)
                    .rotate(DIAMOND_ROTATION_DEGREES)
                    .background(AppPalette.Gold),
        )
        if (onClick != null && portraitBase64 == null) {
            IconBadge(
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp),
                size = 30.dp,
                background = AppPalette.Gold,
                border = AppPalette.Surface,
                borderWidth = 3.dp,
            ) {
                Text("+", color = AppPalette.Background, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
