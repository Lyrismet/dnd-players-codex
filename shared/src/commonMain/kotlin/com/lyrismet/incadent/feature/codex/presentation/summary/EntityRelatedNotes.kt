package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.NumberLabel
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.NumberSizeLadder
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.feature.codex.presentation.summary.RelatedNoteItem
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_related_count_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_empty_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_meta_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EntityRelatedNotesSection(
    entityTitle: String,
    relatedNotes: List<RelatedNoteItem>,
    onRelatedNoteClicked: (sessionNoteId: Long, entryId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = stringResource(Res.string.entity_sheet_related_title),
            trailingContent = {
                Text(
                    stringResource(Res.string.entity_sheet_related_count_format, relatedNotes.size),
                    // mockup resets this counter to weight 500 and no letter-spacing, unlike the section title itself
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = AppPalette.Gold,
                )
            },
            fontSize = AppFontSize.Caption,
        )
        if (relatedNotes.isEmpty()) {
            Text(
                stringResource(Res.string.entity_sheet_related_empty_format, entityTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextTertiary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                relatedNotes.forEach { item ->
                    EntityRelatedNoteRow(item, onClick = { onRelatedNoteClicked(item.sessionNoteId, item.entryId) })
                }
            }
        }
    }
}

@Composable
private fun EntityRelatedNoteRow(
    item: RelatedNoteItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Field, background = AppPalette.Background, onClick = onClick)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberLabel(text = item.numberLabel, sizeLadder = NumberSizeLadder.RELATED_NOTE, lineHeightFactor = 1.3f)
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // mockup - title font-size 14px in the plain Inter face, not the 18sp serif titleMedium
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = AppPalette.TextHeading)
                Text(
                    item.sessionDate.toDisplayDate(currentAppLanguage()),
                    // mockup's date has no bold weight or wide tracking, unlike labelSmall's caption styling
                    style = AppText.caption,
                    color = AppPalette.TextTertiary,
                )
            }
            Text(
                "«${item.snippet}»",
                // mockup - font-size 13px - one step up from bodySmall's 12sp
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body),
                color = AppPalette.TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(Res.string.entity_sheet_related_meta_format, item.matchCount),
                // same de-emphasis as the date above - plain weight, no wide tracking
                style = AppText.caption,
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
