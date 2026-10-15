package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(20.dp)
private val ChipMinHeight = 40.dp
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
    required: Boolean = false,
    maxLength: Int = TextLimits.LINE,
) {
    FormFieldColumn(label, modifier, required) {
        FormInput(
            value = value,
            onChange = { onChange(it.take(maxLength)) },
            placeholder = placeholder,
            maxLength = maxLength,
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
    required: Boolean = false,
    maxLength: Int = TextLimits.NOTE,
) {
    FormFieldColumn(label, modifier, required) {
        FormInput(
            value = value,
            onChange = { onChange(it.take(maxLength)) },
            placeholder = placeholder,
            maxLength = maxLength,
            singleLine = false,
            modifier = Modifier.heightIn(min = AreaMinHeight),
        )
    }
}

/** a small gold link-button under a field - [FormNumberField]'s "same as your character" shortcut */
data class FormFieldAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * labeled integer input on the system number keyboard (Players Codex v7.dc.html `isNum`) - digits only, up to three,
 * a value above the range's end is pulled down at once and one below its start when the field loses focus. An empty
 * field is `null` and shows "—". [action] is an optional link-button under the field.
 */
@Composable
fun FormNumberField(
    label: String,
    value: Int?,
    range: IntRange,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    action: FormFieldAction? = null,
) {
    FormFieldColumn(label, modifier) {
        val textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp, color = AppPalette.TextPrimary)
        BasicTextField(
            value = value?.toString().orEmpty(),
            onValueChange = { typed ->
                val digits = typed.filter { it.isDigit() }.take(NUMBER_FIELD_MAX_DIGITS)
                onChange(digits.toIntOrNull()?.coerceAtMost(range.last))
            },
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = GoldCursorBrush,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(FieldHeight)
                    .appCard(shape = FieldShape, background = AppPalette.Background, border = AppPalette.Border)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && value != null && value < range.first) onChange(range.first)
                    },
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    if (value == null) Text(EMPTY_VALUE_DASH, style = textStyle, color = AppPalette.TextTertiary)
                    innerTextField()
                }
            },
        )
        if (action != null) {
            Text(
                action.label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                color = AppPalette.GoldBright,
                modifier = Modifier.clickable(onClick = action.onClick).padding(vertical = 4.dp),
            )
        }
    }
}

private const val NUMBER_FIELD_MAX_DIGITS = 3

/**
 * a tappable field that opens a select sheet instead of taking text - the picked value shows as a [title] with its
 * [subtitle] under it, as [chips] when several are picked, or as the [placeholder] hint when nothing is.
 */
@Composable
fun FormPickField(
    label: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    chips: (@Composable () -> Unit)? = null,
    leadingGlyph: String? = null,
) {
    FormFieldColumn(label, modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(
                        shape = FieldShape,
                        background = AppPalette.Background,
                        border = AppPalette.Border,
                        onClick = onClick,
                    ).heightIn(min = PickFieldMinHeight)
                    .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingGlyph != null && title != null && chips == null) {
                Text(leadingGlyph, fontSize = 9.sp, color = AppPalette.Parchment)
            }
            Column(modifier = Modifier.weight(1f)) {
                when {
                    chips != null ->
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) { chips() }
                    title != null -> {
                        Text(title, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
                        if (!subtitle.isNullOrBlank()) {
                            Text(subtitle, fontSize = 11.5.sp, color = AppPalette.TextSecondary)
                        }
                    }
                    else ->
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppPalette.TextTertiary,
                        )
                }
            }
            Text("›", fontSize = 18.sp, color = AppPalette.TextTertiary)
        }
    }
}

private val PickFieldMinHeight = 52.dp

@Composable
private fun FormFieldColumn(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = label,
            color = AppPalette.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
            inlineContent =
                if (required) {
                    { Text(" *", fontSize = 10.sp, color = AppPalette.GoldBright) }
                } else {
                    null
                },
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
    maxLength: Int,
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
            val text: @Composable () -> Unit = {
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = AppPalette.TextTertiary)
                    }
                    innerTextField()
                }
            }
            // the counter sits inside the field - centered at the end of a line, bottom end of an area
            if (singleLine) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(FieldHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) { text() }
                    AppCharCountPill(count = value.length, max = maxLength)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth()) {
                    text()
                    AppCharCountPill(count = value.length, max = maxLength, modifier = Modifier.align(Alignment.End))
                }
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
    // mockup chip - min-height 40px, 14px side padding, 13px semibold label
    Box(
        modifier =
            Modifier
                .clip(ChipShape)
                .background(background)
                .border(1.dp, border, ChipShape)
                .clickable { onClick(option.value) }
                .defaultMinSize(minHeight = ChipMinHeight)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            option.label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = foreground,
        )
    }
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
    height: Dp = PrimaryButtonHeight,
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        variant = ButtonVariant.Solid,
        size = ButtonSize.Primary,
        enabled = enabled,
        height = height,
    )
}
