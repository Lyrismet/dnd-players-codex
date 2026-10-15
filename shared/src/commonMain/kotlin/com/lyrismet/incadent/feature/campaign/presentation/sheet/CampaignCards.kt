package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignCardItem
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignLibraryState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_action_finish
import dndplayerscodex.shared.generated.resources.campaign_action_rename
import dndplayerscodex.shared.generated.resources.campaign_badge_archive
import dndplayerscodex.shared.generated.resources.campaign_badge_open
import dndplayerscodex.shared.generated.resources.campaign_library_empty_live
import dndplayerscodex.shared.generated.resources.campaign_section_archived
import dndplayerscodex.shared.generated.resources.campaign_section_live
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(14.dp)
private const val ARCHIVED_ALPHA = 0.85f

/**
 * the running and finished campaigns as the library draws them - the open one is the large card with its own actions
 * (pass them as non-null), the rest are tappable rows.
 */
@Composable
fun CampaignSections(
    library: CampaignLibraryState,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onFinish: (() -> Unit)? = null,
    showEmptyLiveHint: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (library.live.isNotEmpty()) {
            CampaignSectionOverline(
                text = stringResource(Res.string.campaign_section_live, library.live.size),
                color = AppPalette.EmeraldBright,
                topPadding = 2.dp,
            )
            library.live.forEach { item ->
                if (item.isCurrent && (onRename != null || onFinish != null)) {
                    CurrentCampaignCard(item, onRename, onFinish)
                } else {
                    CampaignRow(item, onClick = { onOpen(item.id) })
                }
            }
        } else if (showEmptyLiveHint) {
            Text(
                text = stringResource(Res.string.campaign_library_empty_live),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .appCard(shape = CardShape, background = Color.Transparent, border = AppPalette.Border)
                        .padding(16.dp),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            )
        }
        if (library.archived.isNotEmpty()) {
            CampaignSectionOverline(
                text = stringResource(Res.string.campaign_section_archived, library.archived.size),
                color = AppPalette.TextSecondary,
                topPadding = 8.dp,
            )
            library.archived.forEach { item -> CampaignRow(item, onClick = { onOpen(item.id) }) }
        }
    }
}

@Composable
private fun CurrentCampaignCard(
    item: CampaignCardItem,
    onRename: (() -> Unit)?,
    onFinish: (() -> Unit)?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = CardShape,
                    background = AppPalette.Gold.copy(alpha = 0.08f),
                    border = AppPalette.Gold.copy(alpha = 0.5f),
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampaignTitle(item, nameSize = 18.sp, nameColor = AppPalette.TextHeading, modifier = Modifier.weight(1f))
            TagChip(
                text = stringResource(Res.string.campaign_badge_open),
                foreground = AppPalette.Background,
                background = AppPalette.Gold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (onRename != null) {
                CardAction(stringResource(Res.string.campaign_action_rename), onRename, Modifier.weight(1f))
            }
            if (onFinish != null) {
                CardAction(stringResource(Res.string.campaign_action_finish), onFinish, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CardAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(36.dp)
                .appCard(
                    shape = RoundedCornerShape(10.dp),
                    background = Color.Transparent,
                    border = AppPalette.BorderHover,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = AppPalette.TextDescription,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        )
    }
}

@Composable
private fun CampaignRow(
    item: CampaignCardItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (item.isArchived) ARCHIVED_ALPHA else 1f)
                .appCard(
                    shape = CardShape,
                    background = AppPalette.Background,
                    border = AppPalette.BorderSubtle,
                    onClick = onClick,
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CampaignTitle(
            item = item,
            nameSize = if (item.isArchived) 17.sp else 18.sp,
            nameColor = if (item.isArchived) AppPalette.TextDescription else AppPalette.TextHeading,
            modifier = Modifier.weight(1f),
        )
        if (item.isArchived) {
            val badge =
                stringResource(
                    if (item.isCurrent) Res.string.campaign_badge_open else Res.string.campaign_badge_archive,
                )
            TagChip(
                text = badge,
                foreground = AppPalette.Parchment,
                background = Color.Transparent,
                border = AppPalette.Parchment.copy(alpha = 0.3f),
            )
        } else {
            Text(
                text = item.lastLabel,
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            )
            Text(
                text = "›",
                color = AppPalette.TextTertiary,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            )
        }
    }
}

@Composable
private fun CampaignTitle(
    item: CampaignCardItem,
    nameSize: TextUnit,
    nameColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = nameSize, fontWeight = FontWeight.SemiBold),
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            modifier = Modifier.padding(top = 2.dp),
            color = AppPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CampaignSectionOverline(
    text: String,
    color: Color,
    topPadding: Dp,
) {
    SectionOverline(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = topPadding),
        color = color,
        letterSpacing = 0.14.em,
        fontSize = 10.5.sp,
    )
}
