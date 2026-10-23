package com.lyrismet.incadent.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** the line heights the design repeats, so a screen never declares its own */
object AppLineHeight {
    /** the line of a row's name and of the line under it: they sit on a 20 line so rows keep their rhythm */
    val Row = 20.sp
}

/**
 * the text roles the mockup repeats beyond what the Material slots already name (labelLarge 13/600 is the standard
 * label, bodyMedium 13 the paragraph, bodySmall 12 the hint). Each is a slot plus the mockup's own size.
 */
object AppText {
    /** the primary call to action - 15/700 */
    val action: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.Large, fontWeight = FontWeight.Bold)

    /** a toast or a hint bar - 13 on an 18 line */
    val notice: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body, lineHeight = 18.sp)

    /** the small note under a card or a list - 12.5 on a 17 line */
    val footnote: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus, lineHeight = 17.sp)

    /** "изм.", tab-bar and tip lines - 11 regular */
    val caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Caption)

    /** the tracked all-caps label above a form field or a card block - 10 / 0.14em */
    val fieldOverline: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.labelSmall.copy(fontSize = AppFontSize.Tiny, letterSpacing = 0.14.em)

    /** the name in a row or a card, with the line under it set by [rowSubtitle] - 14.5/600 on a 20 line */
    val rowTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() =
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = AppFontSize.MediumPlus,
                fontWeight = FontWeight.SemiBold,
                lineHeight = AppLineHeight.Row,
            )

    /** the line under a row's name - 12 regular on a 20 line */
    val rowSubtitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small, lineHeight = AppLineHeight.Row)

    /** a short note in a sheet or a panel - 11.5 on a 20 line */
    val rowNote: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Caption, lineHeight = AppLineHeight.Row)
}

/** the line trimmed evenly above and below the glyphs, so a label sits at the true centre of its box */
fun TextStyle.centeredLine(): TextStyle =
    copy(
        lineHeight = fontSize * CENTERED_LINE_RATIO,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

private const val CENTERED_LINE_RATIO = 1.2f
