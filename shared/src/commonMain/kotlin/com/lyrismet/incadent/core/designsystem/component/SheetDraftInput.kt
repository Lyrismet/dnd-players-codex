package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions

/** the single-line "add one more" row of a bottom sheet - 48dp input and a gold "+" square, Done submits too */
@Composable
fun SheetDraftInput(
    draft: String,
    placeholder: String,
    onDraftChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = TextLimits.PHRASE,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppTextInput(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            style = AppTextInputStyle(height = 48.dp),
            maxLength = maxLength,
            keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        IconBadge(
            size = 48.dp,
            shape = AppShapes.Field,
            background = AppPalette.SurfaceInput,
            border = AppPalette.Border,
            onClick = onSubmit,
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge, color = AppPalette.GoldBright)
        }
    }
}
