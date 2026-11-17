package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.GlowingDot
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.NumberSizeLadder
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.session_list_live_continue
import incadent.shared.generated.resources.session_list_live_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveSessionCard(
    session: LiveSessionItem,
    onClick: () -> Unit,
    onMentionClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
    label: String = session.numberLabel,
    // replaces the "continue" link, the edit mode says here that the card stays put
    footerNote: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(16.dp),
                    border = AppPalette.Gold.copy(alpha = 0.45f),
                    onClick = onClick,
                ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LiveCardStatusRow(session.dateLabel)
        LiveCardTitleRow(session, label = label)
        if (session.mentions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.mentions.forEach { chip ->
                    MentionChip(item = chip, onClick = chip.entityRef?.let { ref -> { onMentionClick(ref) } })
                }
            }
        }
        LiveCardFooter(session.notesSummary, footerNote = footerNote)
    }
}

@Composable
internal fun LiveCardStatusRow(
    dateLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlowingDot(dotSize = 7.dp)
        Text(
            stringResource(Res.string.session_list_live_label),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
            color = AppPalette.GoldBright,
            modifier = Modifier.weight(1f),
        )
        Text(
            dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = AppPalette.TextSecondary,
            maxLines = 1,
        )
    }
}

// number and title share a bottom edge, the number's line height is tightened so it doesn't float above the title
@Composable
internal fun LiveCardTitleRow(
    session: LiveSessionItem,
    modifier: Modifier = Modifier,
    label: String = session.numberLabel,
) {
    val numberSize = NumberSizeLadder.LIVE_SESSION.sizeFor(label).sp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            label,
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = numberSize,
                    lineHeight = numberSize * 0.9f,
                ),
            color = AppPalette.Gold,
            maxLines = 1,
            softWrap = false,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(session.overline, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
            Text(session.title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
        }
    }
}

@Composable
internal fun LiveCardFooter(
    notesSummary: String,
    modifier: Modifier = Modifier,
    footerNote: String? = null,
) {
    Column(modifier = modifier) {
        AppDivider(color = AppPalette.Border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(notesSummary, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
            Text(
                footerNote ?: stringResource(Res.string.session_list_live_continue),
                style = MaterialTheme.typography.labelLarge,
                color = if (footerNote == null) AppPalette.GoldBright else AppPalette.TextSecondary,
            )
        }
    }
}
