package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText

/** the accent a button speaks in - gold for the main action, maroon for a destructive or closing one */
enum class ButtonTone { Gold, Maroon, Neutral }

/** [Tinted] is a washed fill with a border, [Solid] a filled one, [Ghost] only a border */
enum class ButtonVariant { Tinted, Solid, Ghost }

/** [Compact] header pills, [Medium] in-card actions, [Large] dialogs and empty states, [Primary] the form action */
enum class ButtonSize { Compact, Medium, Large, Primary }

private class ButtonLook(
    val foreground: Color,
    val background: Color,
    val border: Color?,
)

private fun look(
    tone: ButtonTone,
    variant: ButtonVariant,
    enabled: Boolean,
): ButtonLook =
    if (!enabled) {
        ButtonLook(AppPalette.TextTertiary, AppPalette.SurfaceElevated, null)
    } else {
        when (tone) {
            ButtonTone.Gold ->
                when (variant) {
                    ButtonVariant.Tinted ->
                        ButtonLook(
                            AppPalette.GoldBright,
                            AppPalette.Gold.copy(alpha = 0.12f),
                            AppPalette.Gold.copy(alpha = 0.4f),
                        )
                    ButtonVariant.Solid -> ButtonLook(AppPalette.Background, AppPalette.Gold, AppPalette.Gold)
                    ButtonVariant.Ghost ->
                        ButtonLook(AppPalette.GoldBright, Color.Transparent, AppPalette.Gold.copy(alpha = 0.45f))
                }
            ButtonTone.Maroon ->
                when (variant) {
                    ButtonVariant.Tinted ->
                        ButtonLook(
                            AppPalette.MaroonBright,
                            AppPalette.Maroon.copy(alpha = 0.16f),
                            AppPalette.Maroon.copy(alpha = 0.7f),
                        )
                    ButtonVariant.Solid -> ButtonLook(AppPalette.TextPrimary, AppPalette.Maroon, AppPalette.Maroon)
                    ButtonVariant.Ghost ->
                        ButtonLook(AppPalette.MaroonBright, Color.Transparent, AppPalette.Maroon.copy(alpha = 0.6f))
                }
            ButtonTone.Neutral ->
                when (variant) {
                    ButtonVariant.Solid ->
                        ButtonLook(AppPalette.TextPrimary, AppPalette.SurfaceElevated, AppPalette.BorderHover)
                    ButtonVariant.Tinted, ButtonVariant.Ghost ->
                        ButtonLook(AppPalette.TextDescription, Color.Transparent, AppPalette.BorderHover)
                }
        }
    }

private class ButtonMetrics(
    val height: Dp,
    val radius: Dp,
    val horizontalPadding: Dp,
    val exactHeight: Boolean,
)

private fun metrics(size: ButtonSize): ButtonMetrics =
    when (size) {
        ButtonSize.Compact -> ButtonMetrics(30.dp, 10.dp, 12.dp, exactHeight = true)
        ButtonSize.Medium -> ButtonMetrics(34.dp, 10.dp, 12.dp, exactHeight = true)
        ButtonSize.Large -> ButtonMetrics(48.dp, 12.dp, 16.dp, exactHeight = false)
        ButtonSize.Primary -> ButtonMetrics(52.dp, 14.dp, 16.dp, exactHeight = true)
    }

@Composable
private fun textStyle(size: ButtonSize): TextStyle =
    when (size) {
        ButtonSize.Compact, ButtonSize.Medium -> MaterialTheme.typography.labelLarge
        ButtonSize.Large -> MaterialTheme.typography.titleSmall
        ButtonSize.Primary ->
            AppText.action
    }

/**
 * the one text button - [tone] and [variant] pick its colors, [size] its height and corners. A disabled button reads
 * as a flat gray and takes no taps. [contentColor] recolors just the label when the tone's own one does not fit, and
 * [leading] puts a glyph or icon before it. [height] replaces the size's height for the rare in-between button.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Gold,
    variant: ButtonVariant = ButtonVariant.Tinted,
    size: ButtonSize = ButtonSize.Compact,
    enabled: Boolean = true,
    contentColor: Color? = null,
    height: Dp? = null,
    leading: (@Composable RowScope.(Color) -> Unit)? = null,
) {
    val look = look(tone, variant, enabled)
    val metrics = metrics(size)
    val shape = RoundedCornerShape(metrics.radius)
    val foreground = if (enabled) contentColor ?: look.foreground else look.foreground
    val box = height ?: metrics.height
    Row(
        modifier =
            modifier
                .then(
                    if (metrics.exactHeight ||
                        height != null
                    ) {
                        Modifier.height(box)
                    } else {
                        Modifier.defaultMinSize(minHeight = box)
                    },
                ).appCard(shape, look.background, look.border, onClick = onClick.takeIf { enabled })
                .semantics { role = Role.Button }
                .padding(horizontal = metrics.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this, foreground)
        Text(text, style = textStyle(size), color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
