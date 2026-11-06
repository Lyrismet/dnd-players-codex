package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable
import androidx.compose.ui.interop.LocalUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberTextSharing(): (String) -> Unit {
    val root = LocalUIViewController.current
    return { text ->
        val share = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        share.popoverPresentationController?.sourceView = root.view
        root.presentViewController(share, animated = true, completion = null)
    }
}
