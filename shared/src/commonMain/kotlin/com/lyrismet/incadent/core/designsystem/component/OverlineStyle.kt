package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** how tight an overline is tracked - [Page] for page eyebrows, [Section] for sheet headers, [Field] for form labels */
enum class OverlineStyle(
    val letterSpacing: TextUnit,
    val fontSize: TextUnit,
) {
    Page(0.18.em, TextUnit.Unspecified),
    Section(0.16.em, TextUnit.Unspecified),
    Field(0.14.em, 10.sp),
}
