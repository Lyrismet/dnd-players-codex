package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.navigation.LocalTabSelected

private val DialogShape = RoundedCornerShape(20.dp)

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = AppPalette.BorderSubtle,
    content: @Composable () -> Unit,
) {
    // a dialog is a window above everything, so it goes with its tab instead of hanging over the next one
    if (!LocalTabSelected.current) return
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier =
                modifier
                    .dismissKeyboardOnTap()
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(DialogShape)
                    .background(AppPalette.Surface)
                    .border(1.dp, borderColor, DialogShape)
                    .padding(top = 20.dp, start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}
