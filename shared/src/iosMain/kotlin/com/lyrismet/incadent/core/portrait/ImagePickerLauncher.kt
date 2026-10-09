package com.lyrismet.incadent.core.portrait

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
actual fun rememberImagePickerLauncher(onPicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val rootViewController = LocalUIViewController.current
    // UIImagePickerController.delegate is a weak reference - held here for the picker's presented lifetime
    val activeDelegate = remember { mutableStateOf<ImagePickerDelegate?>(null) }

    fun launch(sourceType: UIImagePickerControllerSourceType) {
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        val delegate = ImagePickerDelegate(onPicked = onPicked, onFinished = { activeDelegate.value = null })
        activeDelegate.value = delegate
        picker.delegate = delegate
        rootViewController.presentViewController(picker, animated = true, completion = null)
    }

    return ImagePickerLauncher(
        launchCamera = { launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) },
        launchGallery = { launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary) },
    )
}

private class ImagePickerDelegate(
    private val onPicked: (ByteArray) -> Unit,
    private val onFinished: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        image?.let { picked -> UIImageJPEGRepresentation(picked, 1.0)?.let { data -> onPicked(data.toByteArray()) } }
        onFinished()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinished()
    }
}
