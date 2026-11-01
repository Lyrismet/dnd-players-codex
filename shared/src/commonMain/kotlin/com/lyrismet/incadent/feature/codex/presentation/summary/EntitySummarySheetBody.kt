package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppPhotoCropDraftWindow
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.HoldTooShortToast
import com.lyrismet.incadent.core.designsystem.component.LocalHoldTooShort
import com.lyrismet.incadent.core.designsystem.component.PortraitBadge
import com.lyrismet.incadent.core.designsystem.component.PortraitMarkContent
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.language.label
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EditorHoleTracker
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EditorShield
import com.lyrismet.incadent.feature.codex.presentation.quickedit.LocalEditorHoleTracker
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityEmblem
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityEmblemShape
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummarySheetActions
import com.lyrismet.incadent.feature.codex.presentation.summary.FactRow
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.StatusOption
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_life_label
import dndplayerscodex.shared.generated.resources.entity_sheet_portrait_hint
import dndplayerscodex.shared.generated.resources.entity_sheet_relation_label
import dndplayerscodex.shared.generated.resources.entity_sheet_status_label
import dndplayerscodex.shared.generated.resources.quick_edit_hold_too_short
import dndplayerscodex.shared.generated.resources.quick_edit_label_name
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
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth().onGloballyPositioned { editorHole.rootCoordinates = it }) {
        CompositionLocalProvider(
            LocalHoldTooShort provides { holdTooShortTrigger += 1 },
            LocalEditorHoleTracker provides editorHole,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 4.dp)) {
                    PinnedOverline(item, actions, onClose)
                }
                AppDivider(color = if (scrollState.value > 0) AppPalette.BorderSubtle else Color.Transparent)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                            // mockup - 44px bottom padding, the header sits under the drag handle
                            .padding(bottom = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    when (item) {
                        is EntitySummaryItem.NpcSummary -> NpcSummaryBody(item, actions)
                        is EntitySummaryItem.LocationSummary -> LocationSummaryBody(item, actions)
                        is EntitySummaryItem.QuestSummary -> QuestSummaryBody(item, actions)
                        is EntitySummaryItem.PartySummary -> PartySummaryBody(item, actions)
                        is EntitySummaryItem.SpeciesSummary -> SpeciesSummaryBody(item, actions)
                    }
                }
            }
        }
        // while an editor is open the rest of the sheet is shielded - taps outside the editor cancel it
        if (quick != null && quick.inlineEdit != null) {
            EditorShield(hole = editorHole.hole, onOutsideTap = { quick.onEvent(QuickEditUiEvent.EditCancelled) })
        }
        quick?.photoCrop?.let { draft ->
            AppPhotoCropDraftWindow(
                draft = draft,
                onConfirm = { crop -> quick.onEvent(QuickEditUiEvent.PhotoCropConfirmed(crop)) },
                onCancel = { quick.onEvent(QuickEditUiEvent.PhotoCropCancelled) },
            )
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
) {
    EntityHeader(
        emblem = npc.emblem,
        title = npc.name,
        subtitle = npc.subtitle,
        showEditHint = actions.onEditClicked != null,
        strikeThrough = npc.isDead,
        quick = actions.quickEdit,
    )
    val relationTitle = stringResource(Res.string.entity_sheet_relation_label)
    val lifeTitle = stringResource(Res.string.entity_sheet_life_label)
    if (actions.statusesReadOnly) {
        EntityStatusSeals(npc.sealsWith(relationTitle, lifeTitle))
    } else {
        EntityStatusSection(
            title = relationTitle,
            options = npc.statusOptions,
            onSelected = { status -> actions.onNpcStatusSelected(npc.ref.id, status) },
        )
        EntityStatusSection(
            title = lifeTitle,
            options = npc.lifeOptions,
            onSelected = { life -> actions.onNpcLifeSelected(npc.ref.id, life) },
        )
    }
    EntityDescription(npc.description, npc.descriptionSegments, actions)
    LinkOfferStrip(actions.linkOffers, actions)
    EntityFactsGrid(
        facts = npc.facts,
        onEntityRefClicked = actions.onEntityRefClicked,
        quick = actions.quickEdit,
        footer = {
            NpcVulnerabilitySection(
                vulnerabilities = npc.vulnerabilities,
                onChange =
                    actions.onNpcVulnerabilityChanged?.let { changed ->
                        { type, state -> changed(npc.ref.id, type, state) }
                    },
                onEdited =
                    actions.onNpcVulnerabilitiesEdited?.let { edited -> { before -> edited(npc.ref.id, before) } },
            )
        },
    )
    EntityRelationGroups(npc.groups, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelatedNotesSection(npc.name, npc.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(npc.ref, actions)
}

@Composable
private fun LocationSummaryBody(
    location: EntitySummaryItem.LocationSummary,
    actions: EntitySummarySheetActions,
) {
    // a read-only card without a cover has nothing to show there, a quick-mode one offers the add strip
    val hasCover = location.cover != null || actions.quickEdit != null
    val coverSlot: @Composable () -> Unit = {
        EntityCoverSlot(
            cover = location.cover,
            title = location.name,
            quick = actions.quickEdit,
            showEditHint = actions.onEditClicked != null,
        )
    }
    EntityHeader(
        emblem = location.emblem,
        title = location.name,
        subtitle = location.subtitle,
        showEditHint = actions.onEditClicked != null,
        quick = actions.quickEdit,
        cover = if (hasCover) coverSlot else null,
    )
    EntityDescription(location.description, location.descriptionSegments, actions)
    LinkOfferStrip(actions.linkOffers, actions)
    EntityFactsGrid(location.facts, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelationGroups(location.groups, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelatedNotesSection(location.name, location.relatedNotes, actions.onRelatedNoteClicked)
    // the two buttons share one block, so they sit close instead of one list gap apart
    if ((actions.quickEdit != null && location.canMove) || actions.onDeleteClicked != null) {
        Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EntityPlaceMoveButton(location, actions.quickEdit)
            EntityDeleteButton(location.ref, actions)
        }
    }
}

@Composable
private fun QuestSummaryBody(
    quest: EntitySummaryItem.QuestSummary,
    actions: EntitySummarySheetActions,
) {
    EntityHeader(
        emblem = quest.emblem,
        title = quest.title,
        subtitle = quest.subtitle,
        showEditHint = actions.onEditClicked != null,
        quick = actions.quickEdit,
    )
    val statusTitle = stringResource(Res.string.entity_sheet_status_label)
    if (actions.statusesReadOnly) {
        quest.statusOptions.firstOrNull { it.isSelected }?.let {
            EntityStatusSeals(
                listOf(StatusSeal(statusTitle, it)),
            )
        }
    } else {
        EntityStatusSection(
            title = statusTitle,
            options = quest.statusOptions,
            onSelected = { status -> actions.onQuestStatusSelected(quest.ref.id, status) },
        )
    }
    EntityDescription(quest.description, quest.descriptionSegments, actions)
    LinkOfferStrip(actions.linkOffers, actions)
    EntityFactsGrid(quest.facts, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelationGroups(quest.groups, actions.onEntityRefClicked, actions.quickEdit)
    EntityRelatedNotesSection(quest.title, quest.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(quest.ref, actions)
}

/** the sheet's top block - overline row with its actions, the optional hold tip, then emblem and title */
@Composable
internal fun EntityHeader(
    emblem: EntityEmblem,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    strikeThrough: Boolean = false,
    showEditHint: Boolean = false,
    quick: QuickEditSheet? = null,
    // a place's cover - sits between the overline row and the emblem, like the mockup's `sh.hasCover`
    cover: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // mockup - the overline's -6dp and the tip's -4dp margins leave 8dp above the tip and 18dp below it
        if (quick.hasVisibleHoldTip()) {
            Spacer(Modifier.height(8.dp))
            HoldTipSlot(quick)
            Spacer(Modifier.height(if (cover != null) 10.dp else 18.dp))
        } else {
            // mockup - the overline's -6dp margin leaves 12dp before the header row, a cover's -4dp margin 8dp
            Spacer(Modifier.height(if (cover != null) 8.dp else 12.dp))
        }
        if (cover != null) {
            cover()
            Spacer(Modifier.height(14.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emblem.hasPortraitSlot) {
                EntityPortraitSlot(emblem, title, quick, showEditHint = showEditHint)
            } else {
                EntityEmblemBox(emblem)
            }
            EntityTitleBlock(
                title = title,
                subtitle = subtitle,
                strikeThrough = strikeThrough,
                quick = quick,
                showPortraitHint = emblem.hasPortraitSlot && emblem.portrait == null && quick != null,
                modifier = Modifier.weight(1f),
            )
        }
    }
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
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = AppFontSize.EntityTitle,
                        lineHeight = 32.sp,
                    ),
                color = AppPalette.TextHeading,
                textDecoration = if (strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                // mockup - font-size 13px - one step up from bodySmall's 12sp
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body),
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (showPortraitHint) {
            Text(
                stringResource(Res.string.entity_sheet_portrait_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small, lineHeight = 17.sp),
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
    val shape = if (emblem.shape == EntityEmblemShape.CIRCLE) CircleShape else AppShapes.Card
    PortraitBadge(
        portrait = emblem.portrait,
        color = emblem.color,
        size = 58.dp,
        shape = shape,
        monochrome = emblem.isMonochrome,
        modifier = modifier,
    ) {
        PortraitMarkContent(
            emblem.mark,
            emblem.color.foreground,
            MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
        )
    }
}

/** the picker of a status family - a read-only card shows [EntityStatusSeals] instead */
@Composable
internal fun <T> EntityStatusSection(
    title: String,
    options: List<StatusOption<T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = title,
            fontSize = AppFontSize.Caption,
        )
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
                style = MaterialTheme.typography.labelMedium.copy(fontSize = AppFontSize.Small),
                color = if (option.isSelected) option.color.foreground else AppPalette.TextSecondary,
            )
        }
    }
}

@Composable
internal fun EntityFactsGrid(
    facts: List<FactRow>,
    onEntityRefClicked: (EntityRef) -> Unit,
    quick: QuickEditSheet? = null,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    // a read-only card has nothing to hold on to, so an empty nicknames row would only be noise
    val shown = if (quick == null) facts.filterNot { it.isEmptyNicknames() } else facts
    if (shown.isEmpty() && footer == null) return
    // mockup - font-size 14px on the facts card, 6px vertical padding, 1px BorderSubtle, 14px radius
    val factTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Card, background = AppPalette.Background)
                .padding(vertical = 6.dp),
    ) {
        shown.forEach { fact -> FactLine(fact, factTextStyle, onEntityRefClicked, quick) }
        if (footer != null) {
            if (shown.isNotEmpty()) {
                AppDivider(
                    color = AppPalette.BorderSubtle,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            footer()
        }
    }
}

private fun FactRow.isEmptyNicknames(): Boolean =
    edit == QuickEditField.NICKNAMES && (value as? FactValue.Text)?.text.isNullOrBlank()
