package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.bleedHorizontally
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.phrase_completion_frequent_meta_format
import dndplayerscodex.shared.generated.resources.phrase_completion_header
import dndplayerscodex.shared.generated.resources.phrase_completion_pinned_meta
import dndplayerscodex.shared.generated.resources.phrase_completion_tab
import dndplayerscodex.shared.generated.resources.phrase_strip_open_button
import org.jetbrains.compose.resources.stringResource

private val StripChipShape = RoundedCornerShape(18.dp)
private val StripChipMaxWidth = 230.dp
private const val PINNED_BORDER_ALPHA = 0.35f
private const val PINNED_GLYPH = "★"
private const val FREQUENT_GLYPH = "↻"

/** the row of pinned and frequent phrases above the input - a tap inserts one, the dashed button opens the sheet */
@Composable
internal fun PhraseStrip(
    items: List<SessionPhraseItem>,
    onPick: (String) -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .bleedHorizontally(COMPOSER_SIDE_PADDING)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = COMPOSER_SIDE_PADDING),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item -> PhraseChip(item, onClick = { onPick(item.text) }) }
        AppDashedTile(onClick = onOpenSheet, cornerRadius = 18.dp, minHeight = 36.dp) {
            Text(
                stringResource(Res.string.phrase_strip_open_button),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.SmallPlus),
                color = AppPalette.GoldBright,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PhraseChip(
    item: SessionPhraseItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(36.dp)
                .widthIn(max = StripChipMaxWidth)
                .appCard(
                    shape = StripChipShape,
                    background = AppPalette.SurfaceInput,
                    border =
                        if (item.isPinned) {
                            AppPalette.Gold.copy(
                                alpha = PINNED_BORDER_ALPHA,
                            )
                        } else {
                            AppPalette.Border
                        },
                    onClick = onClick,
                ).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhraseGlyph(item.isPinned)
        Text(
            item.label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.SmallPlus),
            color = AppPalette.TextDescription,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (!item.isPinned) {
            Text(frequentMeta(item), fontSize = AppFontSize.Caption, color = AppPalette.TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun frequentMeta(item: SessionPhraseItem): String =
    stringResource(Res.string.phrase_completion_frequent_meta_format, item.entryCount)

@Composable
private fun PhraseGlyph(isPinned: Boolean) {
    Text(
        if (isPinned) PINNED_GLYPH else FREQUENT_GLYPH,
        fontSize = AppFontSize.Tiny,
        color = if (isPinned) AppPalette.GoldBright else AppPalette.Azure,
    )
}

/** the "ПРОДОЛЖИТЬ ФРАЗУ" popover body - phrases that continue what is being typed, Tab takes the first one */
@Composable
internal fun PhraseCompletionsContent(
    items: List<SessionPhraseItem>,
    onPick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(Res.string.phrase_completion_header),
            style = AppText.fieldOverline,
            color = AppPalette.TextTertiary,
        )
        Text(
            stringResource(Res.string.phrase_completion_tab),
            fontSize = AppFontSize.Tiny,
            color = AppPalette.TextTertiary,
        )
    }
    items.forEach { item ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.Tile)
                    .clickable { onPick(item.text) }
                    .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhraseGlyph(item.isPinned)
            Text(
                item.label,
                fontSize = AppFontSize.Medium,
                fontWeight = FontWeight.Normal,
                color = AppPalette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (item.isPinned) stringResource(Res.string.phrase_completion_pinned_meta) else frequentMeta(item),
                fontSize = AppFontSize.Caption,
                color = AppPalette.TextSecondary,
            )
        }
    }
}
