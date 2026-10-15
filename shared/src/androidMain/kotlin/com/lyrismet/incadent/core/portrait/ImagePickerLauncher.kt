package com.lyrismet.incadent.core.portrait

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val CAMERA_DIR = "camera"
private const val CAMERA_FILE = "capture.jpg"

@Composable
actual fun rememberImagePickerLauncher(onPicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    // the full-size shot is written to a cache file through the app's FileProvider, the compressor reads its EXIF turn
    val captureFile = remember { mutableStateOf<File?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val file = captureFile.value
            captureFile.value = null
            if (saved && file != null && file.exists()) onPicked(file.readBytes())
            file?.delete()
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { picked ->
                context.contentResolver.openInputStream(picked)?.use { stream -> onPicked(stream.readBytes()) }
            }
        }
    return ImagePickerLauncher(
        launchCamera = {
            val file = File(File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }, CAMERA_FILE)
            captureFile.value = file
            cameraLauncher.launch(context.cameraUri(file))
        },
        launchGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

private fun Context.cameraUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
