package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions

private val ChipShape = RoundedCornerShape(20.dp)
private val QuickAddPanelShape = RoundedCornerShape(12.dp)
private val QuickAddFieldShape = RoundedCornerShape(10.dp)
private val QuickAddFieldSize = 44.dp
private val QuickAddKeyboardClearance = 24.dp
private const val QUICK_ADD_BORDER_ALPHA = 0.6f
private const val QUICK_ADD_PANEL_FILL_ALPHA = 0.06f
private const val QUICK_ADD_PANEL_BORDER_ALPHA = 0.35f
private const val QUICK_ADD_INPUT_BORDER_ALPHA = 0.5f
private const val DASH_LENGTH_PX = 6f
private const val DASH_GAP_PX = 4f

/**
 * optional "+ New NPC"/"+ New place" affordance appended to a [FormChipPicker]'s chip row (Players Codex
 * v6.dc.html's `quick`/`quickCreate`, lines ~2041-2047 and 766-767) - a dashed-gold pill that opens a one-line
 * name input, saved by Enter/✓ or dismissed by ✕. The caller (a presenter/controller) owns [isOpen] and [draft].
 */
data class FormChipQuickAdd(
    val addLabel: String,
    val isOpen: Boolean,
    val draft: String,
    val placeholder: String,
    val helperText: String,
    val onOpen: () -> Unit,
    val onDraftChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
)

/** the dashed-gold "+ New X" pill, styled like a chip but never filled and never selected */
@Composable
internal fun QuickAddPill(quickAdd: FormChipQuickAdd) {
    Text(
        quickAdd.addLabel,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = AppPalette.GoldBright,
        modifier =
            Modifier
                .clip(ChipShape)
                .dashedBorder(AppPalette.Gold.copy(alpha = QUICK_ADD_BORDER_ALPHA))
                .clickable { quickAdd.onOpen() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/**
 * the name input + ✓/✕ that replaces the quick-add pill once opened - autofocused, Enter saves like ✓.
 * [bringIntoViewRequester] pulls the whole panel (not just the focused field) past the keyboard on open, since
 * the system's own focus-driven autoscroll only guarantees the input itself is visible, leaving the helper
 * text below it hidden under the keyboard.
 */
@Composable
internal fun FormChipQuickAddPanel(quickAdd: FormChipQuickAdd) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) { bringIntoViewRequester.bringIntoView() }
    // the spacer is part of the requested region, so the helper text ends up clear of the keyboard edge
    Column(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester)) {
        QuickAddPanelBody(quickAdd)
        Spacer(Modifier.height(QuickAddKeyboardClearance))
    }
}

@Composable
private fun QuickAddPanelBody(quickAdd: FormChipQuickAdd) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = QuickAddPanelShape,
                    background = AppPalette.Gold.copy(alpha = QUICK_ADD_PANEL_FILL_ALPHA),
                    border = AppPalette.Gold.copy(alpha = QUICK_ADD_PANEL_BORDER_ALPHA),
                ).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QuickAddInput(quickAdd, modifier = Modifier.weight(1f))
            IconBadge(
                size = QuickAddFieldSize,
                shape = QuickAddFieldShape,
                background = AppPalette.Gold,
                onClick = quickAdd.onSave,
            ) {
                Text("✓", style = MaterialTheme.typography.titleMedium, color = AppPalette.Background)
            }
            IconBadge(
                size = QuickAddFieldSize,
                shape = QuickAddFieldShape,
                background = Color.Transparent,
                border = AppPalette.BorderHover,
                onClick = quickAdd.onCancel,
            ) {
                Text("✕", style = MaterialTheme.typography.bodySmall, color = AppPalette.TextMuted)
            }
        }
        Text(
            quickAdd.helperText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = AppPalette.TextSecondary,
        )
    }
}

/** the quick-add name field itself - autofocused, Enter saves exactly like a tap on ✓ */
@Composable
private fun QuickAddInput(
    quickAdd: FormChipQuickAdd,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = quickAdd.draft,
        onValueChange = quickAdd.onDraftChanged,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
        cursorBrush = GoldCursorBrush,
        keyboardOptions = SentenceKeyboardOptions,
        keyboardActions = KeyboardActions(onDone = { quickAdd.onSave() }),
        modifier =
            modifier
                .height(QuickAddFieldSize)
                .focusRequester(focusRequester)
                .appCard(
                    shape = QuickAddFieldShape,
                    background = AppPalette.Background,
                    border = AppPalette.Gold.copy(alpha = QUICK_ADD_INPUT_BORDER_ALPHA),
                ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (quickAdd.draft.isEmpty()) {
                    Text(
                        quickAdd.placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppPalette.TextTertiary,
                    )
                }
                innerTextField()
            }
        },
    )
}

/** dashed rounded-rect border - [Modifier.border] has no dash support, the quick-add pill is this file's only user */
private fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    cornerRadius: Dp = 20.dp,
): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style =
                Stroke(
                    width = width.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_LENGTH_PX, DASH_GAP_PX)),
                ),
        )
    }
