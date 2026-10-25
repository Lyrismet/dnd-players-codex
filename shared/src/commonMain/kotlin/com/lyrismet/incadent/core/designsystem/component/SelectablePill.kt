package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor

/** [Small] a tab-like chip in a row, [Regular] a pick in a wrapping group of options */
enum class PillSize(
    val minHeight: Dp,
    val horizontalPadding: Dp,
) {
    Small(30.dp, 12.dp),
    Regular(40.dp, 14.dp),
}

/** what a selected pill is filled with when it has no [StatusColor] of its own */
enum class PillFill {
    /** gold with dark text - the one chosen value of a row */
    Solid,

    /** a washed gold with gold text - one of several ticked options */
    GoldTint,

    /** a neutral raised fill - the "nothing" option of a list */
    Neutral,
}

private const val TINT_FILL_ALPHA = 0.14f

/**
 * the rounded pill that is either picked or not - the one shape of a tab chip, a form option, a link choice and a tag
 * toggle. A selected pill is filled by [selectedFill], or takes [selectedColor] when the option carries its own
 * (a status or an entity color). [background] and [unselectedContent] dress the not picked state.
 */
@Composable
fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PillSize = PillSize.Regular,
    selectedFill: PillFill = PillFill.Solid,
    selectedColor: StatusColor? = null,
    background: Color = Color.Transparent,
    unselectedContent: Color = AppPalette.TextDescription,
) {
    val look = pillLook(selected, selectedFill, selectedColor, background, unselectedContent)
    val shape = remember(size) { RoundedCornerShape(size.minHeight / 2) }
    val base = MaterialTheme.typography.bodySmall
    val textStyle =
        remember(base, size) {
            base.copy(
                fontSize = if (size == PillSize.Small) 12.sp else 13.sp,
                fontWeight = if (size == PillSize.Small) FontWeight.Medium else FontWeight.SemiBold,
            )
        }
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = size.minHeight)
                .appCard(shape, look.background, look.border, onClick = onClick)
                .semantics {
                    role = Role.Button
                    this.selected = selected
                }.padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = textStyle,
            color = look.foreground,
        )
    }
}

private class PillLook(
    val foreground: Color,
    val background: Color,
    val border: Color,
)

private fun pillLook(
    selected: Boolean,
    fill: PillFill,
    color: StatusColor?,
    background: Color,
    unselectedContent: Color,
): PillLook =
    when {
        !selected -> PillLook(unselectedContent, background, AppPalette.Border)
        color != null -> PillLook(color.foreground, color.background, color.border)
        else ->
            when (fill) {
                PillFill.Solid -> PillLook(AppPalette.Background, AppPalette.Gold, AppPalette.Gold)
                PillFill.GoldTint ->
                    PillLook(AppPalette.GoldBright, AppPalette.Gold.copy(alpha = TINT_FILL_ALPHA), AppPalette.Gold)
                PillFill.Neutral -> PillLook(AppPalette.TextMuted, AppPalette.SurfacePopover, AppPalette.BorderHover)
            }
    }
