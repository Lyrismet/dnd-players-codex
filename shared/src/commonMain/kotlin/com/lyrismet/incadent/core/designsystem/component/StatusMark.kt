package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons

/** the mark of an action's outcome, a spinner while work runs, a green tick when it worked, a red cross if not */
@Composable
fun StatusMark(
    kind: StatusKind,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    when (kind) {
        StatusKind.Working -> AppLoader(modifier, size, strokeWidth = 2.dp)
        StatusKind.Success ->
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = AppPalette.EmeraldBright,
                modifier = modifier.size(size),
            )
        StatusKind.Failure ->
            Icon(
                AppIcons.Close,
                contentDescription = null,
                tint = AppPalette.MaroonBright,
                modifier = modifier.size(size),
            )
    }
}
