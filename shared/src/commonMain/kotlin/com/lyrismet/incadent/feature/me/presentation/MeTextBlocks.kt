package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_about_alignment
import dndplayerscodex.shared.generated.resources.me_about_background
import dndplayerscodex.shared.generated.resources.me_about_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TextBlock(
    title: org.jetbrains.compose.resources.StringResource,
    placeholder: org.jetbrains.compose.resources.StringResource,
    value: String,
    onChange: (String) -> Unit,
) {
    FormTextArea(
        label = stringResource(title),
        value = value,
        onChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = stringResource(placeholder),
    )
}

@Composable
internal fun AboutBlock(
    sheet: MeSheetState,
    onClick: () -> Unit,
) {
    val rows =
        listOf(
            stringResource(Res.string.me_about_background) to sheet.background,
            stringResource(Res.string.me_about_alignment) to sheet.alignment,
        ).filter { it.second.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(stringResource(Res.string.me_about_title), color = AppPalette.TextTertiary)
        Column(
            Modifier
                .fillMaxWidth()
                .appCard()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        ) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) AppDivider(color = AppPalette.RowDivider)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.TextSecondary,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppPalette.TextPrimary,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
