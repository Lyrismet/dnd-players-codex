package com.lyrismet.incadent.core.designsystem.component

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.StatusColor

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(20.dp)
private val PrimaryButtonShape = RoundedCornerShape(14.dp)
private val PrimaryButtonHeight = 52.dp
private val FieldHeight = 48.dp
private val AreaMinHeight = 92.dp
private const val AREA_MIN_LINES = 3
private const val SELECTED_CHIP_FILL_ALPHA = 0.14f

/** one chip option for [FormChipPicker] - a single pill, selected state driven by the caller */
data class FormChipOption<T>(
    val value: T,
    val label: String,
    val selected: Boolean,
    /** tints the pill when selected, null keeps the plain gold selection style */
    val selectedColor: StatusColor? = null,
)

/**
 * single-line text input matching the create/edit form's field style (Players Codex v5.dc.html `isText`):
 * 48dp tall, bordered, placeholder support - reused across every codex form field, not just one feature's own form.
 */
@Composable
fun FormTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    FormFieldColumn(label, modifier) {
        FormInput(
            value = value,
            onChange = onChange,
            placeholder = placeholder,
            singleLine = true,
            modifier = Modifier.height(FieldHeight),
        )
    }
}

/** multi-line variant of [FormTextField] (mockup's `isArea`) - same chrome, grows from a 3-line minimum */
@Composable
fun FormTextArea(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    FormFieldColumn(label, modifier) {
        FormInput(
            value = value,
            onChange = onChange,
            placeholder = placeholder,
            singleLine = false,
            modifier = Modifier.heightIn(min = AreaMinHeight),
        )
    }
}

/**
 * labeled integer stepper with −/+ buttons that stop at the ends of [range] - for bounded numeric fields.
 * [onOpenCalculator], when non-null, makes the centered value tappable to open the quick-entry number pad
 * (Players Codex v6.dc.html's `onPad` - only wired for fields whose range has no negative minimum).
 */
@Composable
fun FormStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCalculator: (() -> Unit)? = null,
) {
    FormFieldColumn(label, modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(FieldHeight)
                    .appCard(shape = FieldShape, background = AppPalette.Background, border = AppPalette.Border)
                    .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(symbol = "−", enabled = value > range.first) { onChange(value - 1) }
            val tappable = if (onOpenCalculator != null) Modifier.clickable(onClick = onOpenCalculator) else Modifier
            Text(
                value.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).then(tappable),
            )
            StepperButton(symbol = "+", enabled = value < range.last) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconBadge(
        size = FieldHeight - 8.dp,
        shape = RoundedCornerShape(10.dp),
        background = AppPalette.SurfaceElevated,
        onClick = if (enabled) onClick else null,
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) AppPalette.GoldBright else AppPalette.TextTertiary,
        )
    }
}

@Composable
private fun FormFieldColumn(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = label,
            color = AppPalette.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        )
        content()
    }
}

// BasicTextField, not Material3's TextField, whose 56dp minimum height and 16dp inset don't match the design
@Composable
private fun FormInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String?,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp, color = AppPalette.TextPrimary)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else AREA_MIN_LINES,
        textStyle = textStyle,
        cursorBrush = GoldCursorBrush,
        keyboardOptions = SentenceKeyboardOptions,
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = FieldShape, background = AppPalette.Background, border = AppPalette.Border),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = textStyle, color = AppPalette.TextTertiary)
                }
                innerTextField()
            }
        },
    )
}

/**
 * the pill-chip picker used for every chip-type form field (status, relationship, "given by", "where") - the
 * mockup draws all of these with the identical rounded-pill style, never a segmented tab row. [quickAdd], when
 * non-null, appends a "+ New X" pill after the options that opens an inline name input (see [FormChipQuickAdd]) -
 * every existing caller leaves it null and keeps today's exact rendering.
 */
@Composable
fun <T> FormChipPicker(
    label: String,
    options: List<FormChipOption<T>>,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    quickAdd: FormChipQuickAdd? = null,
) {
    if (options.isEmpty() && quickAdd == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(text = label, color = AppPalette.TextTertiary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option -> ChipOptionPill(option, onClick) }
            if (quickAdd != null && !quickAdd.isOpen) {
                QuickAddPill(quickAdd)
            }
        }
        if (quickAdd != null && quickAdd.isOpen) {
            FormChipQuickAddPanel(quickAdd)
        }
    }
}

/** one selectable pill of [FormChipPicker]'s row - gold or status-tinted fill when selected, plain border otherwise */
@Composable
private fun <T> ChipOptionPill(
    option: FormChipOption<T>,
    onClick: (T) -> Unit,
) {
    val tint = option.selectedColor.takeIf { option.selected }
    val background =
        when {
            tint != null -> tint.background
            option.selected -> AppPalette.Gold.copy(alpha = SELECTED_CHIP_FILL_ALPHA)
            else -> Color.Transparent
        }
    val foreground = tint?.foreground ?: if (option.selected) AppPalette.GoldBright else AppPalette.TextMuted
    val border = tint?.border ?: if (option.selected) AppPalette.Gold else AppPalette.Border
    Text(
        option.label,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = foreground,
        modifier =
            Modifier
                .clip(ChipShape)
                .background(background)
                .border(1.dp, border, ChipShape)
                .clickable { onClick(option.value) }
                .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/**
 * the gold full-width CTA at the bottom of a form/sheet (mockup: "height:52px;border-radius:14px" - the entry
 * form's "Добавить в кодекс"/"Сохранить изменения" and the rename sheet's "Сохранить" share this exact shape) -
 * gold+dark text when enabled, dims to a flat gray when not, per the mockup's `saveBg`/`saveFg` toggle.
 */
@Composable
fun FormPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PrimaryButtonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = AppPalette.Gold,
                contentColor = AppPalette.Background,
                disabledContainerColor = AppPalette.SurfaceElevated,
                disabledContentColor = AppPalette.TextTertiary,
            ),
        modifier = modifier.fillMaxWidth().height(PrimaryButtonHeight),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
    }
}
