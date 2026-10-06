package com.lyrismet.incadent.core.quickedit

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/** knows where the open editor sits inside the sheet root - the shield leaves exactly that rectangle uncovered */
class EditorHoleTracker {
    var rootCoordinates: LayoutCoordinates? = null

    var hole: Rect? by mutableStateOf(null)
        private set

    fun report(coordinates: LayoutCoordinates) {
        val root = rootCoordinates ?: return
        if (coordinates.isAttached && root.isAttached) hole = root.localBoundingBoxOf(coordinates)
    }

    fun clear() {
        hole = null
    }
}

val LocalEditorHoleTracker = compositionLocalOf<EditorHoleTracker?> { null }
