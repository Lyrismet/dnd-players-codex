package com.lyrismet.incadent.core.portrait

import androidx.compose.runtime.Composable

/** opens the platform camera or gallery picker and reports the raw picked image bytes through [onPicked] */
class ImagePickerLauncher(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
)

@Composable
expect fun rememberImagePickerLauncher(onPicked: (ByteArray) -> Unit): ImagePickerLauncher
