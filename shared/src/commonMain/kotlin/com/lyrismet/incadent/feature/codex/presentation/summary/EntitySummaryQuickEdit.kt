package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldHintCard
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.feature.codex.presentation.quickedit.CreatureTypeEditor
import com.lyrismet.incadent.feature.codex.presentation.quickedit.InlineFieldEditor
import com.lyrismet.incadent.feature.codex.presentation.quickedit.LinkOptionEditor
import com.lyrismet.incadent.feature.codex.presentation.quickedit.NotesEditorScreen
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditKind
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.quickedit.editsInSheet
import com.lyrismet.incadent.feature.codex.presentation.quickedit.inputKind
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.FactRow
import com.lyrismet.incadent.feature.codex.presentation.summary.FactValue
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.quick_edit_description_placeholder
import dndplayerscodex.shared.generated.resources.quick_edit_hold_tip
import dndplayerscodex.shared.generated.resources.quick_edit_hold_tip_dismiss
import dndplayerscodex.shared.generated.resources.quick_edit_label_description
import org.jetbrains.compose.resources.stringResource

// the fact label column is a fixed 104px in the mockup, so every value lines up across the card
private val FactLabelWidth = 104.dp
private val FactLabelGap = 12.dp

/** the single source of truth for whether the hold-tip hint should take up space and render */
internal fun QuickEditSheet?.hasVisibleHoldTip(): Boolean = this?.holdTipVisible == true

/** the one-time hint under the card header - shown only while [QuickEditSheet.holdTipVisible] */
@Composable
internal fun HoldTipSlot(quick: QuickEditSheet?) {
    if (!quick.hasVisibleHoldTip()) return
    val sheet = quick ?: return
    HoldHintCard(
        text = stringResource(Res.string.quick_edit_hold_tip),
        dismissLabel = stringResource(Res.string.quick_edit_hold_tip_dismiss),
        onDismiss = { sheet.onEvent(QuickEditUiEvent.HoldHintDismissed) },
    )
}

/** a title or description that opens its in-place editor on a completed hold - read-only without [quick] */
@Composable
internal fun QuickEditableValue(
    field: QuickEditField,
    label: String,
    value: String,
    quick: QuickEditSheet?,
    content: @Composable () -> Unit,
) {
    if (quick == null) {
        content()
        return
    }
    val editing = quick.inlineEdit?.takeIf { it.field == field }
    val notesSource = quick.mention.takeIf { field == QuickEditField.DESCRIPTION }
    if (editing != null && notesSource != null) {
        NotesEditorScreen(editing.draft, notesSource, quick.onEvent)
        content()
    } else if (editing != null) {
        InlineFieldEditor(
            label = label,
            draft = editing.draft,
            kind = field.inputKind(),
            onEvent = quick.onEvent,
        )
    } else {
        HoldToEditField(
            onHeld = { quick.onEvent(QuickEditUiEvent.FieldHeld(field, value)) },
            frame = if (field == QuickEditField.DESCRIPTION) HoldFrame.Note else HoldFrame.Title,
        ) {
            content()
        }
    }
}

/** the entity's description - in quick mode a blank one still shows a hint that holding it adds text */
@Composable
internal fun EntityDescription(
    description: String,
    segments: List<MentionSegment>,
    actions: EntitySummarySheetActions,
) {
    val quick = actions.quickEdit
    val blank = description.isBlank()
    if (quick == null && blank) return
    val shown = if (blank) stringResource(Res.string.quick_edit_description_placeholder) else description
    val label = stringResource(Res.string.quick_edit_label_description)
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = AppFontSize.Large, lineHeight = 24.sp)
    QuickEditableValue(field = QuickEditField.DESCRIPTION, label = label, value = description, quick = quick) {
        val color = if (blank) AppPalette.TextTertiary else AppPalette.TextDescription
        ExpandableNote(shown, segments, style, color, actions.onEntityRefClicked)
    }
}

/** one fact row - a plain line, or a held line that turns into a full-width editor (text, number or link) */
@Composable
internal fun FactLine(
    fact: FactRow,
    style: TextStyle,
    onEntityRefClicked: (EntityRef) -> Unit,
    quick: QuickEditSheet?,
) {
    val field = fact.edit
    // a place or species field is edited in a select sheet, so the line stays as it is while that sheet is open
    val editing =
        quick?.inlineEdit?.takeIf { field != null && it.field == field && !field.editsInSheet }
    when {
        quick == null || field == null ->
            Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                FactRowContent(fact, style, onEntityRefClicked)
            }
        editing != null -> FactEditor(fact, field, quick)
        else ->
            HoldToEditField(
                onHeld = { quick.onEvent(QuickEditUiEvent.FieldHeld(field, currentText(fact))) },
                frame = HoldFrame.FactLine,
            ) {
                // a tap on a chip opens that entity, a hold anywhere on the line opens its editor
                FactRowContent(fact, style, onEntityRefClicked)
            }
    }
}

@Composable
private fun FactRowContent(
    fact: FactRow,
    style: TextStyle,
    onEntityRefClicked: ((EntityRef) -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(fact.label),
            style = style,
            color = AppPalette.TextTertiary,
            modifier = Modifier.width(FactLabelWidth),
        )
        Spacer(Modifier.width(FactLabelGap))
        Box(modifier = Modifier.weight(1f)) {
            FactReadOnlyValue(fact.value, style, onEntityRefClicked)
        }
    }
}

@Composable
private fun FactEditor(
    fact: FactRow,
    field: QuickEditField,
    quick: QuickEditSheet,
) {
    val label = stringResource(fact.label)
    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        when (field.kind) {
            QuickEditKind.Link ->
                LinkOptionEditor(
                    label = label,
                    options = fact.linkOptions,
                    selectedId = (fact.value as? FactValue.Link)?.chip?.entityRef?.id,
                    onEvent = quick.onEvent,
                )
            QuickEditKind.Choice -> CreatureTypeEditor(label, fact.creatureOptions, quick.onEvent)
            else ->
                InlineFieldEditor(
                    label = label,
                    draft = quick.inlineEdit?.draft.orEmpty(),
                    kind = field.inputKind(),
                    onEvent = quick.onEvent,
                )
        }
    }
}

private fun currentText(fact: FactRow): String = (fact.value as? FactValue.Text)?.text.orEmpty()

@Composable
internal fun FactReadOnlyValue(
    value: FactValue,
    style: TextStyle,
    onEntityRefClicked: ((EntityRef) -> Unit)?,
) {
    when (value) {
        is FactValue.Text ->
            Text(value.text.orDash(), style = style, color = AppPalette.TextPrimary)

        is FactValue.Links ->
            if (value.chips.isEmpty()) {
                Text(EMPTY_VALUE_DASH, style = style, color = AppPalette.TextPrimary)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    value.chips.forEach { chip ->
                        MentionChip(
                            item = chip,
                            onClick =
                                onEntityRefClicked?.let { click ->
                                    chip.entityRef?.let { ref -> { click(ref) } }
                                },
                        )
                    }
                }
            }

        is FactValue.Link ->
            MentionChip(
                item = value.chip,
                onClick = onEntityRefClicked?.let { onClick -> value.chip.entityRef?.let { ref -> { onClick(ref) } } },
            )
    }
}
