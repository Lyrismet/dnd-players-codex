package com.lyrismet.incadent.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.RadioOptionRow
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.domain.model.EntityEditMode
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_edit_mode_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_form
import dndplayerscodex.shared.generated.resources.settings_edit_mode_form_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_quick
import dndplayerscodex.shared.generated.resources.settings_edit_mode_quick_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_title
import dndplayerscodex.shared.generated.resources.settings_section_codex
import org.jetbrains.compose.resources.stringResource

/** the Codex group - mockup - overline, then a Surface card with a title block and one radio row per mode */
@Composable
internal fun CodexSection(
    editMode: EntityEditMode,
    onEditModeSelected: (EntityEditMode) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionOverline(
            text = stringResource(Res.string.settings_section_codex),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = AppPalette.TextSecondary,
            letterSpacing = 0.16.em,
            fontSize = 11.sp,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(shape = RoundedCornerShape(14.dp), background = AppPalette.Surface)
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ModeTitleBlock(
                title = stringResource(Res.string.settings_edit_mode_title),
                description = stringResource(Res.string.settings_edit_mode_desc),
            )
            RadioOptionRow(
                title = stringResource(Res.string.settings_edit_mode_quick),
                description = stringResource(Res.string.settings_edit_mode_quick_desc),
                selected = editMode == EntityEditMode.QUICK,
                onClick = { onEditModeSelected(EntityEditMode.QUICK) },
            )
            RadioOptionRow(
                title = stringResource(Res.string.settings_edit_mode_form),
                description = stringResource(Res.string.settings_edit_mode_form_desc),
                selected = editMode == EntityEditMode.FORM,
                onClick = { onEditModeSelected(EntityEditMode.FORM) },
            )
        }
    }
}

// mockup - title 15px/600 #E2E8F0, description 13px #8A93A6 with a 2px gap
@Composable
private fun ModeTitleBlock(
    title: String,
    description: String,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = AppPalette.TextPrimary,
        )
        Text(
            description,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
            color = AppPalette.TextSecondary,
        )
    }
}
