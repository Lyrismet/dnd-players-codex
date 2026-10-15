package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the tracked all-caps label over a form field or a card block, [required] adds the gold star after it */
@Composable
fun FormFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.TextTertiary,
    fontSize: TextUnit = OverlineStyle.Field.fontSize,
    required: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    SectionOverline(
        text = text,
        modifier = modifier,
        color = color,
        style = OverlineStyle.Field,
        fontSize = fontSize,
        inlineContent = if (required) ({ Text(" *", color = AppPalette.GoldBright) }) else null,
        trailingContent = trailingContent,
    )
}
