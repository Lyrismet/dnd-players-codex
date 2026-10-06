package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.FactRow
import com.lyrismet.incadent.core.entitysummary.FactValue
import com.lyrismet.incadent.core.entitysummary.QuickEditSheet
import com.lyrismet.incadent.core.quickedit.InlineFieldEditor
import com.lyrismet.incadent.core.quickedit.LinkOptionEditor
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditKind
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.core.quickedit.inputKind
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.quick_edit_description_placeholder
import dndplayerscodex.shared.generated.resources.quick_edit_hold_tip
import dndplayerscodex.shared.generated.resources.quick_edit_hold_tip_dismiss
import dndplayerscodex.shared.generated.resources.quick_edit_label_description
import org.jetbrains.compose.resources.stringResource

// the fact label column is a fixed 104px in the mockup, so every value lines up across the card
private val FactLabelWidth = 104.dp
private val FactLabelGap = 12.dp

/** the one-time hint under the card header - shown only while [QuickEditSheet.holdTipVisible] */
@Composable
internal fun HoldTipSlot(quick: QuickEditSheet?) {
    val sheet = quick ?: return
    if (!sheet.holdTipVisible) return
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
    if (editing != null) {
        InlineFieldEditor(label = label, draft = editing.draft, kind = field.inputKind(), onEvent = quick.onEvent)
    } else {
        HoldToEditField(
            onHeld = { quick.onEvent(QuickEditUiEvent.FieldHeld(field, value)) },
            frame = if (field == QuickEditField.DESCRIPTION) HoldFrame.Description else HoldFrame.Title,
        ) {
            content()
        }
    }
}

/** the entity's description - in quick mode a blank one still shows a hint that holding it adds text */
@Composable
internal fun EntityDescription(
    description: String,
    quick: QuickEditSheet?,
) {
    val blank = description.isBlank()
    if (quick == null && blank) return
    val shown = if (blank) stringResource(Res.string.quick_edit_description_placeholder) else description
    val label = stringResource(Res.string.quick_edit_label_description)
    QuickEditableValue(field = QuickEditField.DESCRIPTION, label = label, value = description, quick = quick) {
        Text(
            shown,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp),
            color = if (blank) AppPalette.TextTertiary else AppPalette.TextDescription,
        )
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
    val editing = quick?.inlineEdit?.takeIf { field != null && it.field == field }
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
                // quick mode turns link chips into plain text - a hold opens the picker instead of navigating
                FactRowContent(fact, style, onEntityRefClicked = null)
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
            Text(value.text.ifBlank { "—" }, style = style, color = AppPalette.TextPrimary)

        is FactValue.Link ->
            MentionChip(
                item = value.chip,
                onClick = onEntityRefClicked?.let { onClick -> value.chip.entityRef?.let { ref -> { onClick(ref) } } },
            )
    }
}
