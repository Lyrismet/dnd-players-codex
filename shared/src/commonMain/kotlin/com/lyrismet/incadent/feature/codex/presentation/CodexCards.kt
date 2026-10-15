package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.PortraitBadge
import com.lyrismet.incadent.core.designsystem.component.StatusBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmap
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import org.jetbrains.compose.resources.stringResource

/** the shared card chrome - a tappable surface row with a leading badge and a weighted text column */
@Composable
private fun CodexCardRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
        badge()
        Column(modifier = Modifier.weight(1f), content = content)
    }
}

@Composable
private fun CodexInitialBadge(
    initial: String,
    color: StatusColor,
    portrait: Photo? = null,
    monochrome: Boolean = false,
) {
    PortraitBadge(
        portrait = portrait,
        color = color,
        size = 44.dp,
        shape = CircleShape,
        monochrome = monochrome,
    ) {
        Text(initial, style = MaterialTheme.typography.titleLarge, color = color.foreground)
    }
}

@Composable
internal fun NpcCodexCard(
    item: NpcCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CodexCardRow(
        onClick = onClick,
        modifier = modifier,
        badge = {
            CodexInitialBadge(item.initial, item.statusColor, item.portrait, monochrome = item.isDead)
        },
    ) {
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
            item.lifeBadge?.let { StatusBadge(text = it.label, color = it.color) }
            StatusBadge(text = item.statusLabel, color = item.statusColor)
        }
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
internal fun PartyCodexCard(
    item: PartyCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CodexCardRow(
        onClick = onClick,
        modifier = modifier,
        badge = { CodexInitialBadge(item.initial, item.color, item.portrait) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(text = item.presenceLabel, color = item.presenceColor)
        }
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = item.meta,
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                MentionGlyph.QUEST.symbol,
                fontSize = 10.sp,
                color = item.statusColor.foreground,
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
                QuestFactLabel(stringResource(Res.string.codex_quest_given_by_label))
                QuestFactLabel(stringResource(Res.string.codex_quest_reward_label))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val giver = item.giver
                if (giver == null) {
                    Text(EMPTY_VALUE_DASH, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
                } else {
                    Text(
                        text = giver.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = giver.color.foreground,
                        textDecoration = if (giver.isDead) TextDecoration.LineThrough else TextDecoration.None,
                        modifier =
                            Modifier
                                .clickable { onGiverClick(giver.npcId) }
                                .drawBehind {
                                    val y = size.height - 0.5.dp.toPx()
                                    drawLine(giver.color.foreground, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                },
                    )
                }
                Text(
                    item.reward.orDash(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.reward.isBlank()) AppPalette.TextTertiary else AppPalette.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun QuestFactLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
}

/** the place mark of the badge, drawn instead of typed - the "▲" glyph falls back to a tiny one in some fonts */
@Composable
private fun PlaceTriangle() {
    Canvas(Modifier.size(width = 14.dp, height = 12.dp)) {
        val path =
            Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(path, AppPalette.Parchment)
    }
}

@Composable
internal fun LocationCodexCard(
    item: LocationCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CodexCardRow(
        onClick = onClick,
        modifier = modifier,
        badge = {
            IconBadge(
                size = 44.dp,
                shape = RoundedCornerShape(12.dp),
                background = AppPalette.Parchment.copy(alpha = 0.08f),
                border = AppPalette.Parchment.copy(alpha = 0.3f),
            ) {
                PlaceTriangle()
                // the mockup's badge - the cover fills the 44dp square above the glyph
                item.cover?.let { cover ->
                    // the list draws the thumbnail only
                    val bitmap = rememberPhotoBitmap(cover.thumb)
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        },
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.TextHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // the path wraps instead of cutting off, a deep place name is the whole point of the line
        item.subtitle?.let { path ->
            Text(
                path,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
