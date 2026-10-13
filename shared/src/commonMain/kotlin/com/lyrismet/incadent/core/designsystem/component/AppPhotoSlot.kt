package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.portrait.rememberImagePickerLauncher
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmap
import com.lyrismet.incadent.domain.model.Photo

/** one photo and everything a tap leads to, the viewer or the chooser while empty, null [actions] is read-only */
@Composable
fun AppPhotoSlot(
    photo: Photo?,
    kind: String,
    title: String,
    monochrome: Boolean,
    actions: PhotoSlotActions?,
    showEditHint: Boolean,
    viewerOnTap: Boolean = true,
    content: @Composable (onClick: (() -> Unit)?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    val launcher = rememberImagePickerLauncher(onPicked = { actions?.onPicked?.invoke(it) })
    val onClick: (() -> Unit)? =
        when {
            photo != null && viewerOnTap -> {
                { viewerOpen = true }
            }
            actions != null -> {
                { menuOpen = true }
            }
            else -> null
        }
    content(onClick)
    PortraitSourceMenu(
        expanded = menuOpen,
        onCamera = {
            menuOpen = false
            launcher.launchCamera()
        },
        onGallery = {
            menuOpen = false
            launcher.launchGallery()
        },
        onDismiss = { menuOpen = false },
    )
    if (viewerOpen && photo != null) {
        val image = rememberPhotoBitmap(photo.display)
        AppPhotoViewer(
            image = image,
            kind = kind,
            title = title,
            monochrome = monochrome,
            actions =
                actions?.let {
                    PhotoViewerActions(
                        onReplace = {
                            viewerOpen = false
                            menuOpen = true
                        },
                        onCrop = {
                            viewerOpen = false
                            it.onCrop()
                        },
                        onDelete = {
                            viewerOpen = false
                            it.onDelete()
                        },
                    )
                },
            showEditHint = showEditHint,
            onClose = { viewerOpen = false },
        )
    }
}
