package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.StatusOption

/** one status of a read-only card - the family [title] over the [option] currently set */
internal data class StatusSeal<T>(
    val title: String,
    val option: StatusOption<T>,
)

private val SealShape = RoundedCornerShape(14.dp)
private val DiamondShape = RoundedCornerShape(6.dp)
private const val DIAMOND_ROTATION = 45f

/** the read-only card's statuses as equal-width seals (Players Codex v6 `seals`), shown instead of the pickers */
@Composable
internal fun EntityStatusSeals(
    seals: List<StatusSeal<*>>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        seals.forEach { seal -> StatusSealCard(seal.title, seal.option, Modifier.weight(1f)) }
    }
}

@Composable
private fun StatusSealCard(
    title: String,
    option: StatusOption<*>,
    modifier: Modifier = Modifier,
) {
    val color = option.color
    Row(
        modifier =
            modifier
                .clip(SealShape)
                .background(color.background)
                .border(1.dp, color.border, SealShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(28.dp)
                    .rotate(DIAMOND_ROTATION)
                    .clip(DiamondShape)
                    .background(AppPalette.Background)
                    .border(1.5.dp, color.foreground, DiamondShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                option.glyph,
                modifier = Modifier.rotate(-DIAMOND_ROTATION),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = color.foreground,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.14.em),
                color = AppPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                option.label,
                modifier = Modifier.padding(top = 1.dp),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 19.sp, lineHeight = 23.sp),
                color = color.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun EntitySummaryItem.NpcSummary.sealsWith(
    relationTitle: String,
    lifeTitle: String,
): List<StatusSeal<*>> =
    listOfNotNull(
        statusOptions.firstOrNull { it.isSelected }?.let { StatusSeal(relationTitle, it) },
        lifeOptions.firstOrNull { it.isSelected }?.let { StatusSeal(lifeTitle, it) },
    )
