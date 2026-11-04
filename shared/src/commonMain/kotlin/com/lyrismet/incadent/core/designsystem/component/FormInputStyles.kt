package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** the height of a one-line form input and the pick fields beside it */
val FormFieldHeight = 48.dp

/** the line count of a multi-line form area, for an area that is not drawn by [FormTextArea] itself */
const val FORM_AREA_MIN_LINES = 3

/** the text every form input types in */
@Composable
fun formInputTextStyle() = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp)

/** the look of a one-line form input, public for an input that sits beside another control in a row */
@Composable
fun formLineInputStyle() =
    AppTextInputStyle(
        textStyle = formInputTextStyle(),
        height = FormFieldHeight,
        contentPadding = PaddingValues(horizontal = 14.dp),
    )

/** the look of a multi-line form area, public for an area drawn by another input (the notes with mentions) */
@Composable
fun formAreaInputStyle() =
    AppTextInputStyle(
        textStyle = formInputTextStyle(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    )
