package com.lyrismet.incadent.core.portrait

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLauncher(onPicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    // TakePicturePreview hands back a Bitmap directly - no FileProvider/URI needed for a portrait this small
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let { onPicked(it.toPngBytes()) }
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { picked ->
                context.contentResolver.openInputStream(picked)?.use { stream -> onPicked(stream.readBytes()) }
            }
        }
    return ImagePickerLauncher(
        launchCamera = { cameraLauncher.launch(null) },
        launchGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

// PNG is lossless, so the quality argument is ignored - the compressor re-encodes this as jpeg afterwards
private const val LOSSLESS_QUALITY = 100

private fun Bitmap.toPngBytes(): ByteArray =
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.PNG, LOSSLESS_QUALITY, stream)
        stream.toByteArray()
    }
