package com.lyrismet.dndcodex.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.alegreya_variable
import dndplayerscodex.shared.generated.resources.cormorant_garamond_variable
import dndplayerscodex.shared.generated.resources.inter_variable
import dndplayerscodex.shared.generated.resources.lora_variable
import dndplayerscodex.shared.generated.resources.pt_serif_bold
import dndplayerscodex.shared.generated.resources.pt_serif_regular
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

/** the four heading serifs offered in Settings > Кампания, matching the design's `FONTS` map */
enum class HeadingFont {
    ALEGREYA,
    LORA,
    PT_SERIF,
    CORMORANT_GARAMOND,
}

/** [font] is the injection point for the future serif-picker setting, default matches the design's default */
@Composable
fun headingFontFamily(font: HeadingFont = HeadingFont.ALEGREYA): FontFamily =
    when (font) {
        HeadingFont.ALEGREYA ->
            FontFamily(
                Font(Res.font.alegreya_variable, weight = FontWeight.Medium),
                Font(Res.font.alegreya_variable, weight = FontWeight.SemiBold),
                Font(Res.font.alegreya_variable, weight = FontWeight.Bold),
            )
        HeadingFont.LORA ->
            FontFamily(
                Font(Res.font.lora_variable, weight = FontWeight.Medium),
                Font(Res.font.lora_variable, weight = FontWeight.SemiBold),
                Font(Res.font.lora_variable, weight = FontWeight.Bold),
            )
        // PT Serif ships as static Regular/Bold only (no variable weight axis) - SemiBold requests fall back to Bold
        HeadingFont.PT_SERIF ->
            FontFamily(
                Font(Res.font.pt_serif_regular, weight = FontWeight.Medium),
                Font(Res.font.pt_serif_bold, weight = FontWeight.SemiBold),
                Font(Res.font.pt_serif_bold, weight = FontWeight.Bold),
            )
        HeadingFont.CORMORANT_GARAMOND ->
            FontFamily(
                Font(Res.font.cormorant_garamond_variable, weight = FontWeight.Medium),
                Font(Res.font.cormorant_garamond_variable, weight = FontWeight.SemiBold),
                Font(Res.font.cormorant_garamond_variable, weight = FontWeight.Bold),
            )
    }

/** [headingFontFamily] is the injection point for the future serif-picker setting */
@Composable
fun appTypography(headingFontFamily: FontFamily = headingFontFamily(HeadingFont.ALEGREYA)): Typography {
    val body = interFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = headingFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 37.sp),
        headlineLarge = TextStyle(fontFamily = headingFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = headingFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = headingFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = headingFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp),
        // overline section labels (АРХИВ, БАЗА ЗНАНИЙ...) - the wide tracking is what gives them away
        labelSmall =
            TextStyle(
                fontFamily = body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.18.em,
            ),
    )
}
