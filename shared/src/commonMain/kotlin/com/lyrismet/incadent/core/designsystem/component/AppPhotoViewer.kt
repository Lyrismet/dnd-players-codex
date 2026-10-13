package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.photo_viewer_close
import dndplayerscodex.shared.generated.resources.photo_viewer_crop
import dndplayerscodex.shared.generated.resources.photo_viewer_delete
import dndplayerscodex.shared.generated.resources.photo_viewer_read_only
import dndplayerscodex.shared.generated.resources.photo_viewer_replace
import org.jetbrains.compose.resources.stringResource

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** what a slot's owner does with the photo - the slot itself only runs the picker and the viewer */
class PhotoSlotActions(
    val onPicked: (ByteArray) -> Unit,
    val onCrop: () -> Unit,
    val onDelete: () -> Unit,
)

/** what the viewer's three buttons do - all of them go through the host, which owns the picker and the writes */
data class PhotoViewerActions(
    val onReplace: () -> Unit,
    val onCrop: () -> Unit,
    val onDelete: () -> Unit,
)

/** the photo over the whole screen, null [actions] is the read-only card with no buttons and an optional edit hint */
@Composable
fun AppPhotoViewer(
    image: ImageBitmap,
    kind: String,
    title: String,
    monochrome: Boolean,
    actions: PhotoViewerActions?,
    showEditHint: Boolean,
    onClose: () -> Unit,
) {
    // a window floats above everything, so it goes with its tab like [AppDialog]
    if (!LocalTabSelected.current) return
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AppPalette.Scrim)
                    .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            ViewerHeader(kind, title, onClose)
            Image(
                bitmap = image,
                contentDescription = stringResource(Res.string.photo_viewer_close),
                contentScale = ContentScale.Fit,
                colorFilter = if (monochrome) GrayscaleFilter else null,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable(onClick = onClose),
            )
            if (actions != null) {
                ViewerActionRow(actions)
            } else if (showEditHint) {
                Text(
                    stringResource(Res.string.photo_viewer_read_only),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ViewerHeader(
    kind: String,
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionOverline(text = kind, color = AppPalette.TextTertiary, fontSize = 10.sp, letterSpacing = 0.14.em)
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading, maxLines = 1)
        }
        IconBadge(
            size = 40.dp,
            background = AppPalette.Surface,
            border = AppPalette.BorderHover,
            onClick = onClose,
        ) { Text("✕", color = AppPalette.TextPrimary, fontSize = 16.sp) }
    }
}

@Composable
private fun ViewerActionRow(actions: PhotoViewerActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ViewerButton(stringResource(Res.string.photo_viewer_replace), AppPalette.TextPrimary, actions.onReplace)
        ViewerButton(stringResource(Res.string.photo_viewer_crop), AppPalette.TextPrimary, actions.onCrop)
        ViewerButton(stringResource(Res.string.photo_viewer_delete), AppPalette.MaroonBright, actions.onDelete)
    }
}

@Composable
private fun RowScope.ViewerButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .weight(1f)
                .height(48.dp)
                .appCard(shape = RoundedCornerShape(12.dp), border = AppPalette.BorderHover, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}
