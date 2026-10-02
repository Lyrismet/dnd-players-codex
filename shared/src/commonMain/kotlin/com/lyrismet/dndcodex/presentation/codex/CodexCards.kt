package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.IconBadge
import com.lyrismet.dndcodex.core.designsystem.component.MentionGlyph
import com.lyrismet.dndcodex.core.designsystem.component.StatusBadge
import com.lyrismet.dndcodex.core.designsystem.component.appCard
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
                .appCard(shape = RoundedCornerShape(14.dp), onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            size = 44.dp,
            shape = CircleShape,
            background = AppPalette.Background,
            border = item.statusColor.border,
            borderWidth = 1.5.dp,
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
    onClick: () -> Unit,
    onGiverClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = RoundedCornerShape(14.dp), onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                MentionGlyph.QUEST.symbol,
                fontSize = 10.sp,
                color = item.statusColor.foreground,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextHeading,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(text = item.statusLabel, color = item.statusColor)
        }
        Row(modifier = Modifier.padding(start = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.giver != null) QuestFactLabel(stringResource(Res.string.codex_quest_given_by_label))
                QuestFactLabel(stringResource(Res.string.codex_quest_reward_label))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.giver?.let { giver ->
                    Text(
                        text = giver.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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
                Text(item.reward, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
            }
        }
    }
}

@Composable
private fun QuestFactLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
}

@Composable
internal fun LocationCodexCard(
    item: LocationCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = RoundedCornerShape(14.dp), onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            size = 44.dp,
            shape = RoundedCornerShape(12.dp),
            background = AppPalette.Parchment.copy(alpha = 0.08f),
            border = AppPalette.Parchment.copy(alpha = 0.3f),
        ) {
            Text(MentionGlyph.LOCATION.symbol, color = AppPalette.Parchment)
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
