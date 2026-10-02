package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.GoldCursorBrush
import com.lyrismet.dndcodex.core.designsystem.component.Dot
import com.lyrismet.dndcodex.core.designsystem.component.SectionOverline
import com.lyrismet.dndcodex.core.designsystem.component.SegmentedControl
import com.lyrismet.dndcodex.core.designsystem.component.appCard

@Composable
internal fun CodexSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(42.dp)
                .appCard(shape = RoundedCornerShape(12.dp), border = AppPalette.Border)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).border(1.5.dp, AppPalette.TextTertiary, CircleShape))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppPalette.TextPrimary),
                cursorBrush = GoldCursorBrush,
            )
        }
    }
}

@Composable
internal fun CodexSectionHeader(
    text: String,
    count: Int,
    isFirst: Boolean,
    modifier: Modifier = Modifier,
) {
    SectionOverline(
        text = text,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, top = if (isFirst) 4.dp else 12.dp),
        color = AppPalette.TextSecondary,
        letterSpacing = 0.16.em,
        trailingContent = {
            Text(
                count.toString(),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.em,
                    ),
                color = AppPalette.TextSecondary,
            )
        },
    )
}

internal data class CodexTabRowItem(
    val tab: CodexTab,
    val label: String,
    val count: Int,
)

@Composable
internal fun CodexTabRow(
    items: List<CodexTabRowItem>,
    selected: CodexTab,
    onSelected: (CodexTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedControl(
        items = items,
        onSelected = { onSelected(it.tab) },
        itemBackground = { item ->
            if (item.tab == selected) AppPalette.SurfaceElevated else Color.Transparent
        },
        modifier = modifier,
        itemHeight = 36.dp,
        itemSpacing = 2.dp,
    ) { item ->
        val isSelected = item.tab == selected
        val tint = if (isSelected) AppPalette.GoldBright else AppPalette.TextSecondary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = tint.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
internal fun <T> CodexFilterChipRow(
    items: List<CodexFilterOption<T>>,
    selected: T?,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { option ->
            val isSelected = option.value == selected
            val background = if (isSelected) AppPalette.Gold else AppPalette.Surface
            val foreground = if (isSelected) AppPalette.Background else AppPalette.TextDescription
            val border = if (isSelected) AppPalette.Gold else AppPalette.Border
            Row(
                modifier =
                    Modifier
                        .height(30.dp)
                        .appCard(
                            shape = RoundedCornerShape(15.dp),
                            background = background,
                            border = border,
                            onClick = { onSelected(option.value) },
                        ).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                option.dotColor?.let { dotColor ->
                    Dot(size = 6.dp, color = if (isSelected) foreground else dotColor)
                }
                Text(
                    option.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = foreground,
                )
                Text(
                    option.count.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = foreground.copy(alpha = 0.6f),
                )
            }
        }
    }
}
