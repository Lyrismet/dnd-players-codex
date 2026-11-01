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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmapAsync
import com.lyrismet.incadent.domain.model.Photo

private val PortraitArchShape =
    RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
private val PortraitArchInsetShape =
    RoundedCornerShape(topStart = 39.dp, topEnd = 39.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

// mockup's diamond is a square rotated to stand on one corner, with a 3px halo in the sheet colour
private val DiamondSize = 8.dp
private val DiamondHaloSize = 14.dp

/**
 * the npc/party portrait of the entity sheet header, see Players Codex v6.dc.html's sheet `sh.isPortrait` - an 88x114
 * arch with a gold inset ring and diamond. A decoded [portrait] fills it, otherwise [emblemIcon] or [emblemText]. [onClick]
 * non-null makes it tappable and, while empty, adds the "+" badge that invites a tap.
 */
@Composable
fun PortraitArch(
    portrait: Photo?,
    emblemText: String,
    emblemIcon: ImageVector? = null,
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
            if (portrait != null) {
                val bitmap = rememberPhotoBitmapAsync(portrait.display)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (monochrome) PortraitGrayscaleFilter else null,
                    )
                } else {
                    AppLoader(size = PhotoLoaderSize, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center))
                }
            } else {
                if (emblemIcon != null) {
                    AppEntityIcon(emblemIcon, color.foreground, size = 48.dp)
                } else {
                    Text(
                        emblemText,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = AppFontSize.Hero),
                        color = color.foreground,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(5.dp)
                        .border(1.dp, AppPalette.Gold.copy(alpha = 0.4f), PortraitArchInsetShape),
            )
        }
        ArchDiamond(Modifier.align(Alignment.TopCenter))
        if (onClick != null && portrait == null) {
            AddPortraitBadge(Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun ArchDiamond(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        DiamondFill(
            size = DiamondHaloSize,
            color = AppPalette.Surface,
            modifier = Modifier.offset(y = -(DiamondHaloSize / 2)),
        )
        DiamondFill(
            size = DiamondSize,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = -(DiamondSize / 2)),
        )
    }
}

@Composable
private fun AddPortraitBadge(modifier: Modifier = Modifier) {
    IconBadge(
        modifier = modifier.offset(x = 6.dp, y = 6.dp),
        size = 30.dp,
        background = AppPalette.Gold,
        border = AppPalette.Surface,
        borderWidth = 3.dp,
    ) {
        Text("+", color = AppPalette.Background, fontWeight = FontWeight.Bold, fontSize = AppFontSize.Title)
    }
}
