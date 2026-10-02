package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(20.dp)
private val PrimaryButtonShape = RoundedCornerShape(14.dp)
private val PrimaryButtonHeight = 52.dp

/** one chip option for [FormChipPicker] - a single pill, selected state driven by the caller */
data class FormChipOption<T>(
    val value: T,
    val label: String,
    val selected: Boolean,
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(text = label, color = AppPalette.TextTertiary)
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = placeholder?.let { { Text(it, color = AppPalette.TextTertiary) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
            colors = formFieldColors(),
            shape = FieldShape,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppPalette.Border, FieldShape),
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(text = label, color = AppPalette.TextTertiary)
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = placeholder?.let { { Text(it, color = AppPalette.TextTertiary) } },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
            colors = formFieldColors(),
            shape = FieldShape,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp)
                    .border(1.dp, AppPalette.Border, FieldShape),
        )
    }
}

@Composable
private fun formFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = AppPalette.Background,
        unfocusedContainerColor = AppPalette.Background,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = AppPalette.Gold,
    )

/**
 * the pill-chip picker used for every chip-type form field (status, relationship, "given by", "where") - the
 * mockup draws all of these with the identical rounded-pill style, never a segmented tab row.
 */
@Composable
fun <T> FormChipPicker(
    label: String,
    options: List<FormChipOption<T>>,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(text = label, color = AppPalette.TextTertiary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val background = if (option.selected) AppPalette.Gold.copy(alpha = 0.14f) else Color.Transparent
                val foreground = if (option.selected) AppPalette.GoldBright else AppPalette.TextMuted
                val border = if (option.selected) AppPalette.Gold else AppPalette.Border
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
        }
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
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
