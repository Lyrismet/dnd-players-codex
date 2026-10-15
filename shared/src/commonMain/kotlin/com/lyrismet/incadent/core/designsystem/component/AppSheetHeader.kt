package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette

/**
 * the top of a bottom sheet: an overline (optional, gold unless [overlineColor] says otherwise), the 26sp serif
 * [title], a [subtitle] under it and the round close button. An [intro] paragraph sits below the whole row.
 */
@Composable
fun AppSheetHeader(
    title: AnnotatedString,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    overlineColor: Color = AppPalette.Gold,
    subtitle: String? = null,
    intro: String? = null,
    closeSize: Dp = 40.dp,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (overline != null) {
                    SectionOverline(text = overline, color = overlineColor, style = OverlineStyle.Section)
                }
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = AppFontSize.SheetTitle,
                            lineHeight = 31.sp,
                        ),
                    color = AppPalette.TextHeading,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            SheetCloseButton(onClick = onClose, size = closeSize)
        }
        if (intro != null) {
            Text(intro, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextMuted)
        }
    }
}

/** [AppSheetHeader] for a plain-text title */
@Composable
fun AppSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    overlineColor: Color = AppPalette.Gold,
    subtitle: String? = null,
    intro: String? = null,
    closeSize: Dp = 40.dp,
) {
    AppSheetHeader(AnnotatedString(title), onClose, modifier, overline, overlineColor, subtitle, intro, closeSize)
}
