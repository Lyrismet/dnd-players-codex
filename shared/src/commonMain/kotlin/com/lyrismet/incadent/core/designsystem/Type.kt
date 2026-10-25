package com.lyrismet.incadent.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.alegreya_variable
import dndplayerscodex.shared.generated.resources.inter_variable
import org.jetbrains.compose.resources.Font

/** ui and body text - matches Inter 400/500/600/700 from the design system */
@Composable
fun interFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_variable, weight = FontWeight.Normal),
        Font(Res.font.inter_variable, weight = FontWeight.Medium),
        Font(Res.font.inter_variable, weight = FontWeight.SemiBold),
        Font(Res.font.inter_variable, weight = FontWeight.Bold),
    )

/** headings - Alegreya, pinned by the design, there is no serif picker in Settings */
@Composable
fun headingFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.alegreya_variable, weight = FontWeight.Medium),
        Font(Res.font.alegreya_variable, weight = FontWeight.SemiBold),
        Font(Res.font.alegreya_variable, weight = FontWeight.Bold),
    )

// one flat table of the design's type slots - a longer list of styles, not real complexity
@Suppress("LongMethod")
@Composable
fun appTypography(headingFontFamily: FontFamily = headingFontFamily()): Typography {
    val body = interFontFamily()
    return Typography(
        displayLarge =
            TextStyle(
                fontFamily = headingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Display,
                lineHeight = 37.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = headingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.EntityTitle,
                lineHeight = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = headingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.SectionPlus,
                lineHeight = 28.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = headingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Heading,
                lineHeight = 24.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = headingFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Title,
                lineHeight = AppLineHeight.Row,
            ),
        titleSmall =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Medium,
                lineHeight = AppLineHeight.Row,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.Normal,
                fontSize = AppFontSize.Large,
                lineHeight = AppLineHeight.Row,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.Normal,
                fontSize = AppFontSize.Body,
                lineHeight = AppLineHeight.Row,
            ),
        bodySmall =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.Normal,
                fontSize = AppFontSize.Small,
                lineHeight = AppLineHeight.Row,
            ),
        labelLarge =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Body,
                lineHeight = AppLineHeight.Row,
            ),
        labelMedium =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Caption,
                lineHeight = AppLineHeight.Row,
            ),
        // overline section labels (АРХИВ, БАЗА ЗНАНИЙ...) - the wide tracking is what gives them away
        labelSmall =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.SemiBold,
                fontSize = AppFontSize.Caption,
                lineHeight = AppLineHeight.Row,
                letterSpacing = 0.18.em,
            ),
    )
}
