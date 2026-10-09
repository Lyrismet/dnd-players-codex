package com.lyrismet.incadent.core.portrait

import androidx.compose.ui.graphics.ImageBitmap

/** decodes an encoded image (jpeg/png) into a renderable [ImageBitmap] */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap
