package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.icons.AppIcons
import com.lyrismet.dndcodex.core.entitysummary.EntityEmblem
import com.lyrismet.dndcodex.core.entitysummary.EntityEmblemShape
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import com.lyrismet.dndcodex.core.entitysummary.EntitySummaryItem
import com.lyrismet.dndcodex.core.entitysummary.EntitySummarySheetActions
import com.lyrismet.dndcodex.core.entitysummary.FactRow
import com.lyrismet.dndcodex.core.entitysummary.FactValue
import com.lyrismet.dndcodex.core.entitysummary.RelatedNoteItem
import com.lyrismet.dndcodex.core.entitysummary.RelationGroup
import com.lyrismet.dndcodex.core.entitysummary.StatusOption
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_overline_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_count_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_empty_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_meta_format
import dndplayerscodex.shared.generated.resources.entity_sheet_related_title
import dndplayerscodex.shared.generated.resources.entity_sheet_status_label
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import org.jetbrains.compose.resources.stringResource

@Composable
fun EntitySummarySheetContent(
    item: EntitySummaryItem,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                // mockup's sheet content padding is 6px top / 44px bottom - header sits right under the drag handle
                .padding(top = 6.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        when (item) {
            is EntitySummaryItem.NpcSummary -> NpcSummaryBody(item, actions, onClose)
            is EntitySummaryItem.LocationSummary -> LocationSummaryBody(item, actions, onClose)
            is EntitySummaryItem.QuestSummary -> QuestSummaryBody(item, actions, onClose)
        }
    }
}

@Composable
private fun NpcSummaryBody(
    npc: EntitySummaryItem.NpcSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeaderRow(
        emblem = npc.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_npc),
        overlineValue = npc.statusLabel,
        title = npc.name,
        subtitle = npc.subtitle,
        strikeThrough = npc.isDead,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(npc.ref) } },
        onClose = onClose,
    )
    EntityStatusSection(npc.statusOptions, onSelected = { status -> actions.onNpcStatusSelected(npc.ref.id, status) })
    Text(npc.description, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
    EntityFactsGrid(npc.facts, actions.onEntityRefClicked)
    EntityRelationGroups(npc.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(npc.name, npc.relatedNotes, actions.onRelatedNoteClicked)
}

@Composable
private fun LocationSummaryBody(
    location: EntitySummaryItem.LocationSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeaderRow(
        emblem = location.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_location),
        overlineValue = location.typeLabel,
        title = location.name,
        subtitle = location.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(location.ref) } },
        onClose = onClose,
    )
    Text(location.description, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
    EntityFactsGrid(location.facts, actions.onEntityRefClicked)
    EntityRelationGroups(location.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(location.name, location.relatedNotes, actions.onRelatedNoteClicked)
}

@Composable
private fun QuestSummaryBody(
    quest: EntitySummaryItem.QuestSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeaderRow(
        emblem = quest.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_quest),
        overlineValue = quest.statusLabel,
        title = quest.title,
        subtitle = quest.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(quest.ref) } },
        onClose = onClose,
    )
    EntityStatusSection(
        quest.statusOptions,
        onSelected = { status -> actions.onQuestStatusSelected(quest.ref.id, status) },
    )
    EntityFactsGrid(quest.facts, actions.onEntityRefClicked)
    EntityRelationGroups(quest.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(quest.title, quest.relatedNotes, actions.onRelatedNoteClicked)
}

private val HeaderButtonSize = 36.dp

@Composable
private fun EntityHeaderRow(
    emblem: EntityEmblem,
    overlineTypeLabel: String,
    overlineValue: String,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strikeThrough: Boolean = false,
    onEditClicked: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityEmblemBox(emblem)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(
                    Res.string.entity_sheet_overline_format,
                    overlineTypeLabel.uppercase(),
                    overlineValue.uppercase(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = emblem.color.foreground,
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = AppPalette.TextHeading,
                textDecoration = if (strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    // mockup: font-size:13px - one step up from bodySmall's 12sp
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // mockup: "align-self:flex-start;display:flex;gap:6px" - pinned to the top, not centered with the text block
        Row(
            modifier = Modifier.align(Alignment.Top),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            onEditClicked?.let { onClick ->
                Box(
                    modifier =
                        Modifier
                            .size(HeaderButtonSize)
                            .clip(CircleShape)
                            .background(AppPalette.Gold.copy(alpha = 0.12f))
                            .border(1.dp, AppPalette.Gold.copy(alpha = 0.45f), CircleShape)
                            .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = null,
                        tint = AppPalette.GoldBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            SheetCloseButton(onClick = onClose, size = HeaderButtonSize)
        }
    }
}

@Composable
private fun EntityEmblemBox(
    emblem: EntityEmblem,
    modifier: Modifier = Modifier,
) {
    val shape = if (emblem.shape == EntityEmblemShape.CIRCLE) CircleShape else RoundedCornerShape(14.dp)
    Box(
        modifier =
            modifier
                .size(58.dp)
                .clip(shape)
                .background(AppPalette.Background)
                .border(1.5.dp, emblem.color.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emblem.text, style = MaterialTheme.typography.headlineMedium, color = emblem.color.foreground)
    }
}

@Composable
private fun <T> EntityStatusSection(
    options: List<StatusOption<T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(text = stringResource(Res.string.entity_sheet_status_label), color = AppPalette.TextTertiary)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppPalette.Background)
                    .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val optionShape = RoundedCornerShape(9.dp)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(optionShape)
                            .background(if (option.isSelected) option.color.background else Color.Transparent)
                            .border(
                                1.dp,
                                if (option.isSelected) option.color.border else Color.Transparent,
                                optionShape,
                            ).clickable { onSelected(option.value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option.label,
                        // mockup: font-size:12px - one step up from labelMedium's 11sp
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = if (option.isSelected) option.color.foreground else AppPalette.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityFactsGrid(
    facts: List<FactRow>,
    onEntityRefClicked: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (facts.isEmpty()) return
    // mockup sets font-size:14px on the whole facts grid container, one size up from the bodyMedium default
    val factTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AppPalette.Background, RoundedCornerShape(14.dp))
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        facts.forEach { fact ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(fact.label),
                    style = factTextStyle,
                    color = AppPalette.TextTertiary,
                    modifier = Modifier.width(120.dp),
                )
                when (val value = fact.value) {
                    is FactValue.Text -> Text(value.text, style = factTextStyle, color = AppPalette.TextPrimary)

                    is FactValue.Link ->
                        MentionChip(
                            item = value.chip,
                            onClick = value.chip.entityRef?.let { ref -> { onEntityRefClicked(ref) } },
                        )
                }
            }
        }
    }
}

@Composable
private fun EntityRelationGroups(
    groups: List<RelationGroup>,
    onEntityRefClicked: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionOverline(text = stringResource(group.title), color = AppPalette.TextTertiary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    group.items.forEach { chip ->
                        MentionChip(item = chip, onClick = chip.entityRef?.let { ref -> { onEntityRefClicked(ref) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityRelatedNotesSection(
    entityTitle: String,
    relatedNotes: List<RelatedNoteItem>,
    onRelatedNoteClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            text = stringResource(Res.string.entity_sheet_related_title),
            color = AppPalette.TextTertiary,
            trailingContent = {
                Text(
                    stringResource(Res.string.entity_sheet_related_count_format, relatedNotes.size),
                    // mockup resets this counter to weight 500 and no letter-spacing, unlike the section title itself
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = AppPalette.Gold,
                )
            },
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
                    EntityRelatedNoteRow(item, onClick = { onRelatedNoteClicked(item.sessionNoteId) })
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
                .clip(RoundedCornerShape(12.dp))
                .background(AppPalette.Background)
                .border(1.dp, AppPalette.BorderSubtle, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            item.numberLabel,
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // mockup: title font-size:14px in the plain Inter face, not the 18sp serif titleMedium
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = AppPalette.TextHeading)
                Text(
                    item.dateLabel,
                    // mockup's date has no bold weight or wide tracking, unlike labelSmall's caption styling
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = AppPalette.TextTertiary,
                )
            }
            Text(
                "«${item.snippet}»",
                // mockup: font-size:13px - one step up from bodySmall's 12sp
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = AppPalette.TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(Res.string.entity_sheet_related_meta_format, item.matchCount),
                // same de-emphasis as the date above - plain weight, no wide tracking
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
