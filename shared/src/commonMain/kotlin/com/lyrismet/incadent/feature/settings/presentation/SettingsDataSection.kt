package com.lyrismet.incadent.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppIconListRow
import com.lyrismet.incadent.core.designsystem.component.ExportIcon
import com.lyrismet.incadent.core.designsystem.component.ImportIcon
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_data_export_hint
import dndplayerscodex.shared.generated.resources.settings_data_export_title
import dndplayerscodex.shared.generated.resources.settings_data_import_hint
import dndplayerscodex.shared.generated.resources.settings_data_import_title
import dndplayerscodex.shared.generated.resources.settings_section_data
import org.jetbrains.compose.resources.stringResource

/** the "ДАННЫЕ" group: the campaign goes out to a file and comes back from one */
@Composable
internal fun DataSection(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_data), topSpacing = 12.dp) {
        AppIconListRow(
            title = stringResource(Res.string.settings_data_export_title),
            subtitle = stringResource(Res.string.settings_data_export_hint),
            icon = ExportIcon,
            onClick = onExportClick,
        )
        AppDivider()
        AppIconListRow(
            title = stringResource(Res.string.settings_data_import_title),
            subtitle = stringResource(Res.string.settings_data_import_hint),
            icon = ImportIcon,
            onClick = onImportClick,
        )
    }
}
