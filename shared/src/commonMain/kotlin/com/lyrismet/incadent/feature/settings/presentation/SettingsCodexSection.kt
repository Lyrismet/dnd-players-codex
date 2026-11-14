package com.lyrismet.incadent.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.settings_follow_level_desc
import incadent.shared.generated.resources.settings_follow_level_title
import incadent.shared.generated.resources.settings_section_codex
import org.jetbrains.compose.resources.stringResource

/** the Codex group with the follow-the-party-level switch, a row like the other settings */
@Composable
internal fun CodexSection(
    followPartyLevel: Boolean,
    sink: (SettingsEvent) -> Unit,
) {
    SettingsGroup(stringResource(Res.string.settings_section_codex), topSpacing = 12.dp) {
        AppListRow(
            title = stringResource(Res.string.settings_follow_level_title),
            subtitle = stringResource(Res.string.settings_follow_level_desc),
        ) {
            AppSwitch(
                checked = followPartyLevel,
                onCheckedChange = { sink(SettingsEvent.FollowPartyLevelToggled(it)) },
            )
        }
    }
}
