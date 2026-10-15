package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.SearchIcon

/**
 * the single-line search input - loupe, text and a ✕ that clears it once something is typed. The codex list uses the
 * defaults (42dp, 14sp, surface fill), the select sheet its own (46dp, 15sp, background fill) - see the mockup's
 * `searchPh` field and the picker's "Найти место" field.
 */
@Composable
fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    textSize: TextUnit = 14.sp,
    background: Color = AppPalette.Surface,
    clearEndInset: Dp = 4.dp,
) {
    AppTextInput(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        style =
            AppTextInputStyle(
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = textSize, lineHeight = textSize * 1.3f),
                chrome = AppInputChrome(background = background),
                height = height,
                contentPadding = PaddingValues(start = 12.dp, end = if (query.isEmpty()) 12.dp else clearEndInset),
            ),
        maxLength = TextLimits.SEARCH,
        leading = { SearchIcon() },
        trailing =
            if (query.isEmpty()) {
                null
            } else {
                {
                    IconBadge(
                        size = 36.dp,
                        shape = RoundedCornerShape(10.dp),
                        background = Color.Transparent,
                        onClick = { onQueryChange("") },
                    ) {
                        Text("✕", fontSize = 14.sp, color = AppPalette.TextSecondary)
                    }
                }
            },
    )
}
