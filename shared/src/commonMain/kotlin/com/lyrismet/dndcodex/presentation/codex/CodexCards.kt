package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
