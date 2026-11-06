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

// the picked folder url carries the access right and outlives the save sheet, a copy made from text would lose it
private var pickedFolder: NSURL? = null

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
    return FileSaver(
        hasDefaultFolder = false,
        pickFolder = {
            present(UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder), asCopy = false)) {
                pickedFolder = it
                onFolder(SaveFolder(it.absoluteString.orEmpty(), it.lastPathComponent.orEmpty()))
            }
        },
        save = { source, target, folder ->
            val dir = pickedFolder?.takeIf { folder != null && it.absoluteString == folder.key }
            if (dir != null) {
                onSaved(copyIntoFolder(source, target, dir))
            } else {
                // no folder chosen, the system export dialog asks for the place and keeps the typed name
                if (copyExport(source, target)) {
                    present(
                        UIDocumentPickerViewController(
                            forExportingURLs = listOf(NSURL.fileURLWithPath(exportPath(target))),
                            asCopy = true,
                        ),
                    ) {
                        removeExportCopy(source, target)
                        onSaved(true)
                    }
                } else {
                    onSaved(false)
                }
            }
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun copyExport(
    source: String,
    target: String,
): Boolean {
    if (source == target) return true
    val manager = NSFileManager.defaultManager
    manager.removeItemAtPath(exportPath(target), error = null)
    return manager.copyItemAtPath(exportPath(source), toPath = exportPath(target), error = null)
}

// the renamed copy is only there for the picker, the export itself stays for sharing
@OptIn(ExperimentalForeignApi::class)
private fun removeExportCopy(
    source: String,
    target: String,
) {
    if (source != target) NSFileManager.defaultManager.removeItemAtPath(exportPath(target), error = null)
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
