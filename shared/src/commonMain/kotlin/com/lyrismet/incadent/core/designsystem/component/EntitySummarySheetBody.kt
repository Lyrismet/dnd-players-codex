package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.entitysummary.EntityEmblem
import com.lyrismet.incadent.core.entitysummary.EntityEmblemShape
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import com.lyrismet.incadent.core.entitysummary.FactRow
import com.lyrismet.incadent.core.entitysummary.QuickEditSheet
import com.lyrismet.incadent.core.entitysummary.RelationGroup
import com.lyrismet.incadent.core.entitysummary.StatusOption
import com.lyrismet.incadent.core.quickedit.EditorHoleTracker
import com.lyrismet.incadent.core.quickedit.EditorShield
import com.lyrismet.incadent.core.quickedit.LocalEditorHoleTracker
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_life_label
import dndplayerscodex.shared.generated.resources.entity_sheet_overline_format
import dndplayerscodex.shared.generated.resources.entity_sheet_portrait_hint
import dndplayerscodex.shared.generated.resources.entity_sheet_relation_label
import dndplayerscodex.shared.generated.resources.entity_sheet_status_label
import dndplayerscodex.shared.generated.resources.quick_edit_hold_too_short
import dndplayerscodex.shared.generated.resources.quick_edit_label_name
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import org.jetbrains.compose.resources.stringResource

private const val HOLD_TOAST_BOTTOM_PADDING_DP = 64

@Composable
fun EntitySummarySheetContent(
    item: EntitySummaryItem,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var holdTooShortTrigger by remember { mutableIntStateOf(0) }
    val editorHole = remember { EditorHoleTracker() }
    val quick = actions.quickEdit
    Box(modifier = modifier.fillMaxWidth().onGloballyPositioned { editorHole.rootCoordinates = it }) {
        CompositionLocalProvider(
            LocalHoldTooShort provides { holdTooShortTrigger += 1 },
            LocalEditorHoleTracker provides editorHole,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        // mockup - 6px top / 44px bottom padding, the header sits under the drag handle
                        .padding(top = 6.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (item) {
                    is EntitySummaryItem.NpcSummary -> NpcSummaryBody(item, actions, onClose)
                    is EntitySummaryItem.LocationSummary -> LocationSummaryBody(item, actions, onClose)
                    is EntitySummaryItem.QuestSummary -> QuestSummaryBody(item, actions, onClose)
                    is EntitySummaryItem.PartySummary -> PartySummaryBody(item, actions, onClose)
                }
            }
        }
        // while an editor is open the rest of the sheet is shielded - taps outside the editor cancel it
        if (quick != null && quick.inlineEdit != null) {
            EditorShield(hole = editorHole.hole, onOutsideTap = { quick.onEvent(QuickEditUiEvent.EditCancelled) })
        }
        HoldTooShortToast(
            trigger = holdTooShortTrigger,
            text = stringResource(Res.string.quick_edit_hold_too_short),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = HOLD_TOAST_BOTTOM_PADDING_DP.dp),
        )
    }
}

@Composable
private fun NpcSummaryBody(
    npc: EntitySummaryItem.NpcSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeader(
        emblem = npc.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_npc),
        overlineValue = npc.statusLabel,
        title = npc.name,
        subtitle = npc.subtitle,
        strikeThrough = npc.isDead,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(npc.ref) } },
        onClose = onClose,
        quick = actions.quickEdit,
    )
    EntityStatusSection(
        title = stringResource(Res.string.entity_sheet_relation_label),
        options = npc.statusOptions,
        onSelected = { status -> actions.onNpcStatusSelected(npc.ref.id, status) },
        readOnly = actions.statusesReadOnly,
    )
    EntityStatusSection(
        title = stringResource(Res.string.entity_sheet_life_label),
        options = npc.lifeOptions,
        onSelected = { life -> actions.onNpcLifeSelected(npc.ref.id, life) },
        readOnly = actions.statusesReadOnly,
    )
    EntityDescription(npc.description, actions.quickEdit)
    EntityFactsGrid(npc.facts, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelationGroups(npc.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(npc.name, npc.relatedNotes, actions.onRelatedNoteClicked)
}

@Composable
private fun LocationSummaryBody(
    location: EntitySummaryItem.LocationSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeader(
        emblem = location.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_location),
        overlineValue = location.typeLabel,
        title = location.name,
        subtitle = location.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(location.ref) } },
        onClose = onClose,
        quick = actions.quickEdit,
    )
    EntityDescription(location.description, actions.quickEdit)
    EntityFactsGrid(location.facts, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelationGroups(location.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(location.name, location.relatedNotes, actions.onRelatedNoteClicked)
}

@Composable
private fun QuestSummaryBody(
    quest: EntitySummaryItem.QuestSummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeader(
        emblem = quest.emblem,
        overlineTypeLabel = stringResource(Res.string.session_detail_mention_type_quest),
        overlineValue = quest.statusLabel,
        title = quest.title,
        subtitle = quest.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(quest.ref) } },
        onClose = onClose,
        quick = actions.quickEdit,
    )
    EntityStatusSection(
        title = stringResource(Res.string.entity_sheet_status_label),
        options = quest.statusOptions,
        onSelected = { status -> actions.onQuestStatusSelected(quest.ref.id, status) },
        readOnly = actions.statusesReadOnly,
    )
    EntityDescription(quest.description, actions.quickEdit)
    EntityFactsGrid(quest.facts, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelationGroups(quest.groups, actions.onEntityRefClicked)
    EntityRelatedNotesSection(quest.title, quest.relatedNotes, actions.onRelatedNoteClicked)
}

private val HeaderButtonSize = 36.dp

/** the sheet's top block - overline row with its actions, the optional hold tip, then emblem and title */
@Composable
internal fun EntityHeader(
    emblem: EntityEmblem,
    overlineTypeLabel: String,
    overlineValue: String,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strikeThrough: Boolean = false,
    onEditClicked: (() -> Unit)? = null,
    quick: QuickEditSheet? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        EntityOverlineRow(
            overline =
                stringResource(
                    Res.string.entity_sheet_overline_format,
                    overlineTypeLabel.uppercase(),
                    overlineValue.uppercase(),
                ),
            color = emblem.color.foreground,
            onEditClicked = onEditClicked,
            onClose = onClose,
        )
        // mockup - the overline's -6dp and the tip's -4dp margins leave 8dp above the tip and 18dp below it
        if (quick.hasVisibleHoldTip()) {
            Spacer(Modifier.height(8.dp))
            HoldTipSlot(quick)
            Spacer(Modifier.height(18.dp))
        } else {
            // mockup - the overline's -6dp margin leaves 12dp before the header row
            Spacer(Modifier.height(12.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emblem.hasPortraitSlot) EntityPortraitSlot(emblem, quick) else EntityEmblemBox(emblem)
            EntityTitleBlock(
                title = title,
                subtitle = subtitle,
                strikeThrough = strikeThrough,
                quick = quick,
                showPortraitHint = emblem.hasPortraitSlot && emblem.portraitBase64 == null && quick != null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EntityOverlineRow(
    overline: String,
    color: Color,
    onEditClicked: (() -> Unit)?,
    onClose: () -> Unit,
) {
    // already uppercased by the caller's string resource, so SectionOverline's own uppercase is a no-op here
    SectionOverline(
        text = overline,
        color = color,
        letterSpacing = 0.16.em,
        trailingContent = {
            // matches the original Row's spacedBy(8.dp), now that the overline and the buttons are split apart
            Spacer(Modifier.width(8.dp))
            onEditClicked?.let { onClick ->
                IconBadge(
                    size = HeaderButtonSize,
                    background = AppPalette.Gold.copy(alpha = 0.12f),
                    border = AppPalette.Gold.copy(alpha = 0.45f),
                    onClick = onClick,
                ) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = null,
                        tint = AppPalette.GoldBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            SheetCloseButton(onClick = onClose, size = HeaderButtonSize)
        },
    )
}

@Composable
private fun EntityTitleBlock(
    title: String,
    subtitle: String,
    strikeThrough: Boolean,
    quick: QuickEditSheet?,
    showPortraitHint: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        QuickEditableValue(
            field = QuickEditField.NAME,
            label = stringResource(Res.string.quick_edit_label_name),
            value = title,
            quick = quick,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 27.sp, lineHeight = 32.sp),
                color = AppPalette.TextHeading,
                textDecoration = if (strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                // mockup - font-size 13px - one step up from bodySmall's 12sp
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (showPortraitHint) {
            Text(
                stringResource(Res.string.entity_sheet_portrait_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EntityEmblemBox(
    emblem: EntityEmblem,
    modifier: Modifier = Modifier,
) {
    val shape = if (emblem.shape == EntityEmblemShape.CIRCLE) CircleShape else RoundedCornerShape(14.dp)
    PortraitBadge(
        portraitBase64 = emblem.portraitBase64,
        color = emblem.color,
        size = 58.dp,
        shape = shape,
        monochrome = emblem.isMonochrome,
        modifier = modifier,
    ) {
        // the design sizes the NPC initial at 26, the quest diamond at 18 and the location triangle at 16
        val textSize =
            when {
                emblem.shape == EntityEmblemShape.CIRCLE -> 26.sp
                emblem.text == MentionGlyph.QUEST.symbol -> 18.sp
                else -> 16.sp
            }
        Text(
            emblem.text,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = textSize),
            color = emblem.color.foreground,
        )
    }
}

/** the picker of a status family - in a read-only card it collapses to a badge of the current value */
@Composable
internal fun <T> EntityStatusSection(
    title: String,
    options: List<StatusOption<T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            text = title,
            color = AppPalette.TextTertiary,
            letterSpacing = 0.14.em,
        )
        if (readOnly) {
            options.firstOrNull { it.isSelected }?.let { current -> StatusBadge(current.label, current.color) }
        } else {
            SegmentedControl(
                items = options,
                onSelected = { onSelected(it.value) },
                itemBackground = { option -> if (option.isSelected) option.color.background else Color.Transparent },
                itemBorder = { option -> if (option.isSelected) option.color.border else Color.Transparent },
                containerBackground = AppPalette.Background,
                itemHeight = 32.dp,
                itemSpacing = 4.dp,
            ) { option ->
                Text(
                    option.label,
                    // mockup - font-size 12px - one step up from labelMedium's 11sp
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (option.isSelected) option.color.foreground else AppPalette.TextSecondary,
                )
            }
        }
    }
}

@Composable
internal fun EntityFactsGrid(
    facts: List<FactRow>,
    onEntityRefClicked: (EntityRef) -> Unit,
    quick: QuickEditSheet? = null,
    modifier: Modifier = Modifier,
) {
    if (facts.isEmpty()) return
    // mockup - font-size 14px on the facts card, 6px vertical padding, 1px BorderSubtle, 14px radius
    val factTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = RoundedCornerShape(14.dp), background = AppPalette.Background)
                .padding(vertical = 6.dp),
    ) {
        facts.forEach { fact -> FactLine(fact, factTextStyle, onEntityRefClicked, quick) }
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
                SectionOverline(
                    text = stringResource(group.title),
                    color = AppPalette.TextTertiary,
                    letterSpacing = 0.14.em,
                )
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
