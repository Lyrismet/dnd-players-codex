package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

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
                .clip(RoundedCornerShape(12.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = AppPalette.TextTertiary)
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppPalette.TextPrimary),
                cursorBrush = SolidColor(AppPalette.Gold),
            )
        }
    }
}

@Composable
internal fun CodexSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = AppPalette.TextTertiary,
        modifier = modifier.padding(top = 14.dp, bottom = 4.dp),
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items.forEach { item ->
            val isSelected = item.tab == selected
            val tint = if (isSelected) AppPalette.GoldBright else AppPalette.TextSecondary
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) AppPalette.SurfaceElevated else Color.Transparent)
                        .clickable { onSelected(item.tab) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tint.copy(alpha = 0.65f),
                    )
                }
            }
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
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { option ->
            val isSelected = option.value == selected
            val background = if (isSelected) AppPalette.Gold else AppPalette.Surface
            val foreground = if (isSelected) AppPalette.Background else AppPalette.TextMuted
            val border = if (isSelected) AppPalette.Gold else AppPalette.BorderSubtle
            Row(
                modifier =
                    Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(background)
                        .border(1.dp, border, RoundedCornerShape(15.dp))
                        .clickable { onSelected(option.value) }
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                option.dotColor?.let { dotColor ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) foreground else dotColor))
                }
                Text(option.label, style = MaterialTheme.typography.bodySmall, color = foreground)
                Text(
                    option.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = foreground.copy(alpha = 0.6f),
                )
            }
        }
    }
}
