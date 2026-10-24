package com.lyrismet.incadent.core.designsystem

import androidx.compose.ui.unit.sp

/**
 * every text size the design uses, named after where the mockup (Players Codex v7) spends it. The mockup is hand
 * tuned, so half sizes are real - 12.5 is a compact action, 11.5 a secondary hint - and are kept, not rounded.
 */
object AppFontSize {
    /** the glyph inside a badge */
    val Micro = 9.sp

    /** field overlines and small tags */
    val Tiny = 10.sp

    /** group overlines and counter pills */
    val TinyPlus = 10.5.sp

    /** tab labels, "изм.", tip lines */
    val Caption = 11.sp

    /** secondary hints under a field */
    val CaptionPlus = 11.5.sp

    /** helper paragraphs and small buttons */
    val Small = 12.sp

    /** compact in-card actions */
    val SmallPlus = 12.5.sp

    /** default text and the standard button label */
    val Body = 13.sp
    val BodyPlus = 13.5.sp

    /** row titles in settings and lists */
    val Medium = 14.sp
    val MediumPlus = 14.5.sp

    /** form text and the primary call to action */
    val Large = 15.sp
    val Lead = 16.sp
    val LeadPlus = 17.sp

    /** card titles */
    val Title = 18.sp
    val TitlePlus = 19.sp

    /** list-item headings */
    val Heading = 20.sp
    val HeadingPlus = 21.sp

    /** empty-state titles */
    val Section = 22.sp
    val SectionPlus = 23.sp
    val SubDisplay = 24.sp

    /** bottom-sheet titles (26) and the title of one entity, a session or a card (27) */
    val SheetTitle = 26.sp
    val EntityTitle = 27.sp

    /** screen titles */
    val Stat = 28.sp
    val Display = 32.sp
    val StatLarge = 34.sp
    val Hero = 36.sp
    val Number = 46.sp
}
