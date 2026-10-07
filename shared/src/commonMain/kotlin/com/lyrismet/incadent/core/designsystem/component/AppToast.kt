package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette

private val AppToastShape = RoundedCornerShape(12.dp)

// the UndoToast chrome without an action - [text] drives it and the last text lingers while it fades out
@Composable
fun AppToast(
    text: String?,
    modifier: Modifier = Modifier,
) {
    var lastText by remember { mutableStateOf(text) }
    LaunchedEffect(text) { if (text != null) lastText = text }

    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        lastText?.let { current ->
            Text(
                current,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = AppPalette.TextPrimary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 10.dp, shape = AppToastShape)
                        .appCard(shape = AppToastShape, background = AppPalette.SurfaceElevated, border = null)
                        .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 10.dp),
            )
        }
    }
}
