package com.lyrismet.incadent.core.portrait

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val CAMERA_DIR = "camera"
private const val CAMERA_FILE = "capture.jpg"

@Composable
actual fun rememberImagePickerLauncher(onPicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // the full-size shot goes to a fixed cache file through the app's FileProvider, so a recreated activity finds it
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val file = context.captureFile()
            scope.launch {
                val bytes =
                    withContext(Dispatchers.IO) {
                        file.takeIf { saved && it.exists() }?.readBytes().also { file.delete() }
                    }
                bytes?.let(onPicked)
            }
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val bytes =
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                bytes?.let(onPicked)
            }
        }
    return ImagePickerLauncher(
        launchCamera = {
            cameraLauncher.launch(context.cameraUri(context.captureFile()))
        },
        launchGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

private fun Context.captureFile(): File = File(File(cacheDir, CAMERA_DIR).apply { mkdirs() }, CAMERA_FILE)

private fun Context.cameraUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
