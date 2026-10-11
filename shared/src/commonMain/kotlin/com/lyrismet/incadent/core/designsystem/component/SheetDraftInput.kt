package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions

/** the single-line "add one more" row of a bottom sheet - 48dp input and a gold "+" square, Done submits too */
@Composable
fun SheetDraftInput(
    draft: String,
    placeholder: String,
    onDraftChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChanged,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
            cursorBrush = GoldCursorBrush,
            keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .appCard(
                        shape = RoundedCornerShape(12.dp),
                        background = AppPalette.Background,
                        border = AppPalette.Border,
                    ),
            decorationBox = { innerTextField ->
                // stacked, not side by side, so the caret sits at the start of the placeholder
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (draft.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextTertiary)
                    }
                    innerTextField()
                }
            },
        )
        IconBadge(
            size = 48.dp,
            shape = RoundedCornerShape(12.dp),
            background = AppPalette.SurfaceInput,
            border = AppPalette.Border,
            onClick = onSubmit,
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge, color = AppPalette.GoldBright)
        }
    }
}
