package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.StatusBadge
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import org.jetbrains.compose.resources.stringResource

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

@Composable
internal fun NpcCodexCard(
    item: NpcCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppPalette.Background)
                    .border(1.5.dp, item.statusColor.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.initial, style = MaterialTheme.typography.titleMedium, color = item.statusColor.foreground)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppPalette.TextHeading,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.isDead) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(text = item.statusLabel, color = item.statusColor)
            }
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun QuestCodexCard(
    item: QuestCodexItem,
    onGiverClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("◆", fontSize = 10.sp, color = item.statusColor.foreground)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(text = item.statusLabel, color = item.statusColor)
        }
        item.giver?.let { giver ->
            Row(
                modifier = Modifier.padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(Res.string.codex_quest_given_by_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.TextTertiary,
                )
                Text(
                    text = giver.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = giver.color.foreground,
                    textDecoration =
                        if (giver.isDead) {
                            TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                        } else {
                            TextDecoration.Underline
                        },
                    modifier = Modifier.clickable { onGiverClick(giver.npcId) },
                )
            }
        }
        Row(modifier = Modifier.padding(start = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(Res.string.codex_quest_reward_label),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
            )
            Text(item.reward, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextPrimary)
        }
    }
}

@Composable
internal fun LocationCodexCard(
    item: LocationCodexItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppPalette.Parchment.copy(alpha = 0.08f))
                    .border(1.dp, AppPalette.Parchment.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = AppPalette.Parchment)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
        }
    }
}
