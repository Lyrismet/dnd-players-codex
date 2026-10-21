package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette

@Composable
internal fun SelectEmptyText(
    texts: AppSelectTexts,
    isListEmpty: Boolean,
    canCreate: Boolean,
) {
    val hint = texts.emptyHint.takeIf { isListEmpty && canCreate }
    Text(
        hint ?: texts.emptyText,
        fontSize = AppFontSize.Body,
        lineHeight = 20.sp,
        color = if (hint != null) AppPalette.TextSecondary else AppPalette.TextTertiary,
        modifier = Modifier.padding(4.dp),
    )
}

@Composable
internal fun SelectGroupHeader(
    title: String,
    isFirst: Boolean,
) {
    Text(
        title.uppercase(),
        fontSize = AppFontSize.Caption,
        letterSpacing = 1.sp,
        color = AppPalette.TextSecondary,
        modifier = Modifier.padding(top = if (isFirst) 12.dp else 16.dp, bottom = 2.dp),
    )
}
