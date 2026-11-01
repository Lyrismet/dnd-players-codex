package com.lyrismet.incadent.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.core.designsystem.component.ListSectionHeader
import com.lyrismet.incadent.core.designsystem.component.RadioOptionRow
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.preferences.EntityEditMode
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_edit_mode_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_form
import dndplayerscodex.shared.generated.resources.settings_edit_mode_form_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_quick
import dndplayerscodex.shared.generated.resources.settings_edit_mode_quick_desc
import dndplayerscodex.shared.generated.resources.settings_edit_mode_title
import dndplayerscodex.shared.generated.resources.settings_follow_level_desc
import dndplayerscodex.shared.generated.resources.settings_follow_level_title
import dndplayerscodex.shared.generated.resources.settings_section_codex
import org.jetbrains.compose.resources.stringResource

/** the Codex group - mockup - overline, then a Surface card with a title block and one radio row per mode */
@Composable
internal fun CodexSection(
    editMode: EntityEditMode,
    followPartyLevel: Boolean,
    sink: (SettingsEvent) -> Unit,
) {
    val onEditModeSelected = { mode: EntityEditMode -> sink(SettingsEvent.EditModeSelected(mode)) }
    val onFollowPartyLevelToggled = { follow: Boolean -> sink(SettingsEvent.FollowPartyLevelToggled(follow)) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ListSectionHeader(text = stringResource(Res.string.settings_section_codex), fontSize = AppFontSize.Caption)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(shape = AppShapes.Card, background = AppPalette.Surface)
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(shape = AppShapes.Card, background = AppPalette.Surface)
                    .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ModeTitleBlock(
                    title = stringResource(Res.string.settings_follow_level_title),
                    description = stringResource(Res.string.settings_follow_level_desc),
                )
            }
            AppSwitch(checked = followPartyLevel, onCheckedChange = onFollowPartyLevelToggled)
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
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontSize = AppFontSize.Large,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = AppPalette.TextPrimary,
        )
        Text(
            description,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body, lineHeight = 19.5.sp),
            color = AppPalette.TextSecondary,
        )
    }
}
