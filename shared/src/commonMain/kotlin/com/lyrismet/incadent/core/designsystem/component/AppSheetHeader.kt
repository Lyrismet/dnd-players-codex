package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the top of a bottom sheet: gold overline (optional), 26sp serif title and the round close button */
@Composable
fun AppSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                SectionOverline(text = overline, color = AppPalette.Gold, letterSpacing = 0.16.em)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 31.sp),
                color = AppPalette.TextHeading,
            )
        }
        SheetCloseButton(onClick = onClose, size = 40.dp)
    }
}
