package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val SheetScrimColor = Color(0xFF050508).copy(alpha = 0.66f)

/** modal bottom sheet matching the design's entity/action sheets - 26dp top corners, dark surface, thin drag handle */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = SheetShape,
        containerColor = AppPalette.Surface,
        scrimColor = SheetScrimColor,
        dragHandle = { AppBottomSheetDragHandle() },
        content = content,
    )
}

@Composable
private fun AppBottomSheetDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.BorderHover),
        )
    }
}
