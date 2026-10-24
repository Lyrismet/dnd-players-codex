package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.PortraitBadge
import com.lyrismet.incadent.core.designsystem.component.StatusBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.cosmicBackdrop
import com.lyrismet.incadent.core.designsystem.component.coverStrip
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmap
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.presentation.style.DamageTypeShield
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import com.lyrismet.incadent.feature.codex.presentation.summary.speciesHitsLine
import com.lyrismet.incadent.feature.codex.presentation.summary.speciesSeenLine
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_place_top_level_caption
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import org.jetbrains.compose.resources.stringResource

// the share of the text column left empty beside a cover strip
private const val RESERVED_END_WEIGHT = 0.6f

/** the shared card chrome - a tappable surface row with a leading badge and a weighted text column */
@Composable
private fun CodexCardRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: Color? = AppPalette.BorderSubtle,
    backdrop: Modifier = Modifier,
    // keeps the text clear of a picture drawn along the card's right edge
    reserveEnd: Boolean = false,
    badge: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Card, border = border, onClick = onClick)
                .then(backdrop)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        badge()
        Column(modifier = Modifier.weight(1f), content = content)
        if (reserveEnd) Spacer(Modifier.weight(RESERVED_END_WEIGHT))
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
internal fun SpeciesCodexCard(
    item: SpeciesCodexItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CodexCardRow(
        onClick = onClick,
        modifier = modifier,
        badge = { CodexInitialBadge(MentionGlyph.SPECIES.symbol, SpeciesMentionColor, item.photo) },
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
            StatusBadge(text = item.creatureTypeLabel, color = SpeciesMentionColor)
        }
        if (item.shields.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item.shields.forEach { (type, state) ->
                    DamageTypeShield(type, state, width = 22.dp)
                }
            }
        }
        val seen =
            listOf(speciesSeenLine(item.seenInSessions, item.fights), speciesHitsLine(item.hitPoints, item.hitRange))
        val seenLine = seen.filter { it.isNotEmpty() }.joinToString(" · ")
        if (seenLine.isNotEmpty()) {
            Text(
                text = seenLine,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
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
                textDecoration = if (item.fallen) TextDecoration.LineThrough else TextDecoration.None,
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
                .appCard(shape = AppShapes.Card, onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                MentionGlyph.QUEST.symbol,
                fontSize = AppFontSize.Tiny,
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

// the edge of a top level place's card, a cool tint that sits on the night-sky fill
private val TopLevelBorder = Color(0x3D96A0E6)

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
    // the list draws the thumbnail only
    val cover = item.cover?.let { rememberPhotoBitmap(it.thumb) }
    CodexCardRow(
        onClick = onClick,
        modifier = modifier,
        border = if (item.isTopLevel) TopLevelBorder else AppPalette.BorderSubtle,
        backdrop =
            (if (item.isTopLevel) Modifier.cosmicBackdrop(item.id) else Modifier)
                .then(cover?.let { Modifier.coverStrip(it) } ?: Modifier),
        reserveEnd = cover != null,
        badge = {
            IconBadge(
                size = 44.dp,
                shape = AppShapes.Field,
                background = AppPalette.Parchment.copy(alpha = 0.08f),
                border = AppPalette.Parchment.copy(alpha = 0.3f),
            ) {
                PlaceTriangle()
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
        val topLevel = stringResource(Res.string.codex_place_top_level_caption)
        (item.subtitle ?: topLevel.takeIf { item.isTopLevel })?.let { path ->
            Text(
                path,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
