package com.lyrismet.incadent.core.file

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
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
import androidx.core.net.toUri
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
actual fun rememberFileSharing(): (String) -> Unit {
    val context = LocalContext.current
    return { name ->
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
    }
}

@Composable
actual fun rememberFileSaver(
    onFolder: (SaveFolder) -> Unit,
    onSaved: (Boolean) -> Unit,
): FileSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogSource = remember { mutableStateOf<String?>(null) }
    // before Android 10 there is no folder that takes a copy unasked, so the system dialog asks for the place
    val dialog =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME)) { uri ->
            val source = dialogSource.value
            dialogSource.value = null
            if (uri == null || source == null) return@rememberLauncherForActivityResult
            scope.launch { onSaved(withContext(Dispatchers.IO) { context.copyExportTo(source, uri) }) }
        }
    val tree =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { onFolder(SaveFolder(it.toString(), context.folderName(it))) }
        }
    return FileSaver(
        hasDefaultFolder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        pickFolder = { tree.launch(null) },
        save = { source, target, folder ->
            when {
                folder != null ->
                    scope.launch {
                        onSaved(withContext(Dispatchers.IO) { context.saveToTree(source, target, folder.key.toUri()) })
                    }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    scope.launch { onSaved(withContext(Dispatchers.IO) { context.saveToDownloads(source, target) }) }
                else -> {
                    dialogSource.value = source
                    dialog.launch(target)
                }
            }
        },
    )
}

private fun Context.copyExportTo(
    source: String,
    uri: Uri,
): Boolean =
    runCatching {
        contentResolver.openOutputStream(uri)?.use { out ->
            exportFile(source).inputStream().use { it.copyTo(out) }
        } != null
    }.getOrDefault(false)

private fun Context.folderName(tree: Uri): String =
    runCatching {
        val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        contentResolver
            .query(folder, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: tree.lastPathSegment.orEmpty()

// the provider renames a clashing file itself, so an older export in the folder is never overwritten
private fun Context.saveToTree(
    source: String,
    target: String,
    tree: Uri,
): Boolean =
    runCatching {
        val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val uri = DocumentsContract.createDocument(contentResolver, folder, JSON_MIME, target)
        val copied = uri != null && copyExportTo(source, uri)
        if (!copied && uri != null) DocumentsContract.deleteDocument(contentResolver, uri)
        copied
    }.getOrDefault(false)

// the store renames a clashing file itself, so an older export is never overwritten
@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.saveToDownloads(
    source: String,
    target: String,
): Boolean =
    runCatching {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, target)
                put(MediaStore.Downloads.MIME_TYPE, JSON_MIME)
            }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        val copied = uri != null && copyExportTo(source, uri)
        if (!copied && uri != null) contentResolver.delete(uri, null, null)
        copied
    }.getOrDefault(false)
