package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions

/** the 56dp serif name field of a naming sheet, focused with the caret after the last letter */
@Composable
fun AppNameInput(
    name: String,
    onNameChanged: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit = {},
) {
    var value by remember { mutableStateOf(TextFieldValue(name, TextRange(name.length))) }
    AppTextInput(
        value = value,
        onValueChange = {
            value = it
            onNameChanged(it.text)
        },
        autoFocus = true,
        placeholder = placeholder,
        style =
            AppTextInputStyle(
                textStyle =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = AppFontSize.Section,
                        fontWeight = FontWeight.SemiBold,
                    ),
                textColor = AppPalette.TextHeading,
                chrome = AppInputChrome(border = AppPalette.Gold.copy(alpha = 0.5f)),
                height = 56.dp,
            ),
        maxLength = TextLimits.LINE,
        counter = AppTextInputCounter(TextLimits.LINE),
        keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}
