package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeData
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
actual fun rememberFileSharing(onSaved: (Boolean) -> Unit): FileSharing {
    val root = LocalUIViewController.current
    val present = rememberPickerPresenter()
    return FileSharing(
        share = { name ->
            val share =
                UIActivityViewController(
                    activityItems = listOf(NSURL.fileURLWithPath(exportPath(name))),
                    applicationActivities = null,
                )
            share.popoverPresentationController?.sourceView = root.view
            root.presentViewController(share, animated = true, completion = null)
        },
        save = { name ->
            present(
                UIDocumentPickerViewController(
                    forExportingURLs = listOf(NSURL.fileURLWithPath(exportPath(name))),
                    asCopy = true,
                ),
            ) {
                onSaved(true)
            }
        },
    )
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
