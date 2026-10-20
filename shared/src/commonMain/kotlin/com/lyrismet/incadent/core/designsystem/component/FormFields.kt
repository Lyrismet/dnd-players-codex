package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH

private val FieldShape = AppShapes.Field
private val PrimaryButtonHeight = 52.dp
private val FieldHeight = 48.dp
private val AreaMinHeight = 92.dp
private const val AREA_MIN_LINES = 3

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
        AppTextInput(
            value = value?.toString().orEmpty(),
            onValueChange = { typed ->
                val digits = typed.filter { it.isDigit() }.take(NUMBER_FIELD_MAX_DIGITS)
                onChange(digits.toIntOrNull()?.coerceAtMost(range.last))
            },
            placeholder = EMPTY_VALUE_DASH,
            style =
                AppTextInputStyle(
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp),
                    height = FieldHeight,
                ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier =
                Modifier.onFocusChanged { focus ->
                    if (!focus.isFocused && value != null && value < range.first) onChange(range.first)
                },
        )
        if (action != null) {
            Text(
                action.label,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = AppFontSize.SmallPlus,
                        fontWeight = FontWeight.SemiBold,
                    ),
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
                Text(leadingGlyph, style = CenteredLine, fontSize = AppFontSize.Micro, color = AppPalette.Parchment)
            }
            Column(modifier = Modifier.weight(1f)) {
                when {
                    chips != null ->
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) { chips() }
                    title != null -> {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyLarge.centered(),
                            color = AppPalette.TextPrimary,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = CenteredLine,
                                fontSize = AppFontSize.CaptionPlus,
                                color = AppPalette.TextSecondary,
                            )
                        }
                    }
                    else ->
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge.centered(),
                            color = AppPalette.TextTertiary,
                        )
                }
            }
            AppChevron()
        }
    }
}

private val PickFieldMinHeight = 52.dp

// the line box is trimmed to the glyphs, so a one-line value sits in the middle of the field and not above it
private val CenteredLine =
    TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both))

private fun TextStyle.centered(): TextStyle = merge(CenteredLine)

@Composable
private fun FormFieldColumn(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(
            text = label,
            required = required,
        )
        content()
    }
}

@Composable
private fun FormInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String?,
    maxLength: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    AppTextInput(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        placeholder = placeholder,
        style =
            AppTextInputStyle(
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp),
                height = if (singleLine) FieldHeight else null,
                contentPadding =
                    if (singleLine) {
                        PaddingValues(
                            horizontal = 14.dp,
                        )
                    } else {
                        PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    },
            ),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else AREA_MIN_LINES,
        counter = AppTextInputCounter(maxLength),
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
        FormFieldLabel(text = label)
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
    SelectablePill(
        text = option.label,
        selected = option.selected,
        onClick = { onClick(option.value) },
        selectedFill = PillFill.GoldTint,
        selectedColor = option.selectedColor,
        unselectedContent = AppPalette.TextMuted,
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
