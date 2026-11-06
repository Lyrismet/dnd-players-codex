package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

/** presents document pickers and keeps the delegate alive, the picker only holds it weakly */
@Composable
private fun rememberPickerPresenter(): (UIDocumentPickerViewController, (NSURL) -> Unit) -> Unit {
    val root = LocalUIViewController.current
    val active = remember { mutableStateOf<PickerDelegate?>(null) }
    return { picker, onUrl ->
        val delegate = PickerDelegate(onUrl) { active.value = null }
        active.value = delegate
        picker.delegate = delegate
        root.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberFilePicker(onPicked: (FileOrigin) -> Unit): () -> Unit {
    val present = rememberPickerPresenter()
    // a copy is made into the app, so the file stays readable after the picker closes
    return {
        present(
            UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON, UTTypeData), asCopy = true),
        ) {
            onPicked(UrlFileOrigin(it))
        }
    }
}

@Composable
actual fun rememberFileSharing(): (String) -> Unit {
    val root = LocalUIViewController.current
    return { name ->
        val share =
            UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(exportPath(name))),
                applicationActivities = null,
            )
        share.popoverPresentationController?.sourceView = root.view
        root.presentViewController(share, animated = true, completion = null)
    }
}

@Composable
actual fun rememberFileSaver(
    onFolder: (SaveFolder) -> Unit,
    onSaved: (Boolean) -> Unit,
): FileSaver {
    val present = rememberPickerPresenter()
    // the picked folder url carries the access right, a copy of it made from text would lose it
    val folderUrl = remember { mutableStateOf<NSURL?>(null) }
    return FileSaver(
        hasDefaultFolder = false,
        pickFolder = {
            present(UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder), asCopy = false)) {
                folderUrl.value = it
                onFolder(SaveFolder(it.absoluteString.orEmpty(), it.lastPathComponent.orEmpty()))
            }
        },
        save = { source, target, folder ->
            val dir = folderUrl.value?.takeIf { folder != null && it.absoluteString == folder.key }
            if (dir != null) {
                onSaved(copyIntoFolder(source, target, dir))
            } else {
                // no folder chosen, the system export dialog asks for the place and keeps the typed name
                copyExport(source, target)
                present(
                    UIDocumentPickerViewController(
                        forExportingURLs = listOf(NSURL.fileURLWithPath(exportPath(target))),
                        asCopy = true,
                    ),
                ) {
                    onSaved(true)
                }
            }
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun copyExport(
    source: String,
    target: String,
) {
    if (source == target) return
    val manager = NSFileManager.defaultManager
    manager.removeItemAtPath(exportPath(target), error = null)
    manager.copyItemAtPath(exportPath(source), toPath = exportPath(target), error = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun copyIntoFolder(
    source: String,
    target: String,
    folder: NSURL,
): Boolean {
    val manager = NSFileManager.defaultManager
    val scoped = folder.startAccessingSecurityScopedResource()
    try {
        // a clashing name gets a number, so an older export in the folder is never overwritten
        val dot = target.indexOf('.', startIndex = 1).takeIf { it > 0 } ?: target.length
        val destination =
            generateSequence(0) { it + 1 }
                .map { if (it == 0) target else "${target.substring(0, dot)} ($it)${target.substring(dot)}" }
                .mapNotNull { folder.URLByAppendingPathComponent(it) }
                .first { !manager.fileExistsAtPath(it.path.orEmpty()) }
        return manager.copyItemAtURL(NSURL.fileURLWithPath(exportPath(source)), toURL = destination, error = null)
    } finally {
        if (scoped) folder.stopAccessingSecurityScopedResource()
    }
}

private class PickerDelegate(
    private val onUrl: (NSURL) -> Unit,
    private val onFinished: () -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(onUrl)
        onFinished()
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onFinished()
}
