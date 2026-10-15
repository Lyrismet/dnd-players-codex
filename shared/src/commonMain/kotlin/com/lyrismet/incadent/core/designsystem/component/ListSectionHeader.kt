package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the header of a list section (codex groups, archive, settings groups) with an optional [count] at the end */
@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    fontSize: TextUnit = OverlineStyle.Section.fontSize,
) {
    SectionOverline(
        text = text,
        modifier = modifier.padding(horizontal = 4.dp),
        color = AppPalette.TextSecondary,
        style = OverlineStyle.Section,
        fontSize = fontSize,
        trailingContent = {
            if (count != null) {
                Text(
                    text = count.toString(),
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.em,
                        ),
                    color = AppPalette.TextSecondary,
                )
            }
        },
    )
}
