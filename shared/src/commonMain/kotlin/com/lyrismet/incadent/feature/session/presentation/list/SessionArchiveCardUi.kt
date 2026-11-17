package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.NumberLabel
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.feature.session.presentation.tags.sessionTagLabel
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.mention_count_notes
import incadent.shared.generated.resources.session_list_edit_new_mark
import incadent.shared.generated.resources.session_list_mention_count_format
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// matches the bordered tag chip's own rendered height (16sp text + 3dp top/bottom padding) so an unbordered
// chip in the same row - the "@N" mention badge - lines up with it instead of sizing a touch shorter
private val SessionCardTagChipHeight = 22.dp

/** the card of a past session, [trailing] being the handle of the edit mode */
@Composable
internal fun ArchiveCard(
    session: SessionListItem,
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfaceVariant,
                    border = AppPalette.BorderSubtle,
                    onClick = onClick,
                ).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberLabel(text = label, width = 52.dp, color = labelColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ArchiveTitle(session.title)
            ArchiveMeta(session)
            ArchiveChips(session)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun ArchiveChips(session: SessionListItem) {
    if (session.tags.isEmpty() && session.mentionCount == 0 && !session.isNew) return
    FlowRow(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (session.isNew) {
            TagChip(
                text = stringResource(Res.string.session_list_edit_new_mark),
                foreground = AppPalette.GoldBright,
                background = AppPalette.Gold.copy(alpha = 0.12f),
                height = SessionCardTagChipHeight,
            )
        }
        session.tags.forEach { tag ->
            TagChip(
                text = sessionTagLabel(tag),
                foreground = AppPalette.TextDescription,
                background = AppPalette.SurfacePopover,
                border = AppPalette.Border,
                height = SessionCardTagChipHeight,
            )
        }
        if (session.mentionCount > 0) {
            val mentionLabel = stringResource(Res.string.session_list_mention_count_format, session.mentionCount)
            TagChip(
                text = mentionLabel,
                foreground = AppPalette.Gold,
                background = AppPalette.Gold.copy(alpha = 0.08f),
                height = SessionCardTagChipHeight,
            )
        }
    }
}

@Composable
internal fun ArchiveTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = AppPalette.TextHeading,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

// the date and how many notes the session holds
@Composable
internal fun ArchiveMeta(session: SessionListItem) {
    val notes = pluralStringResource(Res.plurals.mention_count_notes, session.noteCount, session.noteCount)
    Text(
        joinWithDot(session.dateLabel, notes),
        style = MaterialTheme.typography.bodySmall,
        color = AppPalette.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
