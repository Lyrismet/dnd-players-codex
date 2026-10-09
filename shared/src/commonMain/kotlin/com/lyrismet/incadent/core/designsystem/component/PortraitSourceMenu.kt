package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_source_camera
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_source_gallery
import org.jetbrains.compose.resources.stringResource

/** the "Camera / Gallery" choice opened by tapping an empty or filled [PortraitFrame] - built on [AppDialog] */
@Composable
fun PortraitSourceMenu(
    expanded: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!expanded) return
    AppDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PortraitSourceOption(stringResource(Res.string.codex_entry_portrait_source_camera), onCamera)
            PortraitSourceOption(stringResource(Res.string.codex_entry_portrait_source_gallery), onGallery)
        }
    }
}

@Composable
private fun PortraitSourceOption(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .appCard(shape = RoundedCornerShape(12.dp), onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
