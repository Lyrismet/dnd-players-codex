package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.copy_button_description
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val COPIED_MILLIS = 2000L

// the deprecated manager on purpose, ClipEntry has no common text factory
@Suppress("DEPRECATION")
/** a copy icon that puts [text] on the clipboard and shows a green tick for two seconds once it is there */
@Composable
fun AppCopyButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MILLIS)
            copied = false
        }
    }
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clickable {
                    clipboard.setText(AnnotatedString(text))
                    copied = true
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (copied) AppIcons.Check else AppIcons.Copy,
            contentDescription = stringResource(Res.string.copy_button_description),
            tint = if (copied) AppPalette.EmeraldBright else AppPalette.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
