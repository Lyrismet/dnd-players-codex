package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText

private val CaptionHeight = 12.dp
private val HintSize = 28.dp

/** the small caption of a section with its "?" hint right after it when there is one, the hint does not add height */
@Composable
fun AppSectionTitleHint(
    title: String,
    modifier: Modifier = Modifier,
    helpTitle: String? = null,
    helpBody: String? = null,
) {
    Row(modifier.height(CaptionHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = AppText.fieldOverline, color = AppPalette.TextTertiary)
        if (helpTitle != null && helpBody != null) AppHelpHint(helpTitle, helpBody, Modifier.requiredSize(HintSize))
    }
}
