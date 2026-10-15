package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette

// the UndoToast chrome without an action - [text] drives it and the last text lingers while it fades out
@Composable
fun AppToast(
    text: String?,
    modifier: Modifier = Modifier,
) {
    FloatingBar(value = text, modifier = modifier, shadowed = true) { current ->
        Text(
            current,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = AppPalette.TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
