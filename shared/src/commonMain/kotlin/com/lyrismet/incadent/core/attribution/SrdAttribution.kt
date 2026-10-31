package com.lyrismet.incadent.core.attribution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppLinkedText
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_about_srd_title
import dndplayerscodex.shared.generated.resources.settings_about_srd_value
import org.jetbrains.compose.resources.stringResource

/** the SRD 5.1 credit with its two license links, shown in About and under the species form */
@Composable
fun SrdAttribution(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = stringResource(Res.string.settings_about_srd_title),
            fontSize = AppFontSize.Medium,
            fontWeight = FontWeight.Medium,
            color = AppPalette.TextPrimary,
        )
        AppLinkedText(
            text = stringResource(Res.string.settings_about_srd_value),
            fontSize = AppFontSize.Small,
            color = AppPalette.TextSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
