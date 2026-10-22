package com.lyrismet.incadent.core.file

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val JSON_MIME = "application/json"

// some apps hand a json file over as plain bytes, so the picker shows everything and the file itself is checked
private val PICK_MIMES = arrayOf(JSON_MIME, "application/octet-stream")

@Composable
actual fun rememberFilePicker(onPicked: (FileOrigin) -> Unit): () -> Unit {
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onPicked(UriFileOrigin(context.applicationContext, it)) }
        }
    return { picker.launch(PICK_MIMES) }
}

@Composable
actual fun rememberFileSharing(onSaved: (Boolean) -> Unit): FileSharing {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savingName = remember { mutableStateOf<String?>(null) }
    val saver =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME)) { uri ->
            val name = savingName.value
            savingName.value = null
            if (uri == null || name == null) return@rememberLauncherForActivityResult
            scope.launch {
                val copied =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                context.exportFile(name).inputStream().use { it.copyTo(out) }
                            } != null
                        }.getOrDefault(false)
                    }
                onSaved(copied)
            }
        }
    return FileSharing(
        share = { name ->
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    context.exportFile(name),
                )
            val send =
                Intent(Intent.ACTION_SEND)
                    .setType(JSON_MIME)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        save = { name ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                scope.launch { onSaved(withContext(Dispatchers.IO) { context.saveToDownloads(name) }) }
            } else {
                savingName.value = name
                saver.launch(name)
            }
        },
    )
}

// the store renames a clashing file itself, so an older export is never overwritten
@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.saveToDownloads(name: String): Boolean =
    runCatching {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, JSON_MIME)
            }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        val copied =
            uri != null &&
                contentResolver.openOutputStream(uri)?.use { out ->
                    exportFile(name).inputStream().use { it.copyTo(out) }
                } != null
        if (!copied && uri != null) contentResolver.delete(uri, null, null)
        copied
    }.getOrDefault(false)
