package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.AppDoneButton
import com.lyrismet.incadent.core.designsystem.component.AppEditButton
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.session_list_edit_add_past
import incadent.shared.generated.resources.session_list_edit_banner
import incadent.shared.generated.resources.session_list_edit_between_format
import incadent.shared.generated.resources.session_list_edit_button
import incadent.shared.generated.resources.session_list_edit_done
import incadent.shared.generated.resources.session_list_edit_handle_description
import incadent.shared.generated.resources.session_list_edit_help_body
import incadent.shared.generated.resources.session_list_edit_help_title
import org.jetbrains.compose.resources.stringResource

/** "Edit" while the list is calm and "Done" in the edit mode, at the end of the title line */
@Composable
internal fun EditModeButton(
    editing: Boolean,
    onClick: () -> Unit,
) {
    val text = stringResource(if (editing) Res.string.session_list_edit_done else Res.string.session_list_edit_button)
    if (editing) AppDoneButton(text, onClick) else AppEditButton(text, onClick)
}

/** the plate under the title of the edit mode, with the "?" that tells how the order is made */
@Composable
internal fun EditBanner(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.Gold.copy(alpha = 0.06f),
                    border = AppPalette.Gold.copy(alpha = 0.3f),
                ).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.session_list_edit_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextDescription,
            modifier = Modifier.weight(1f),
        )
        AppHelpHint(
            title = stringResource(Res.string.session_list_edit_help_title),
            body = stringResource(Res.string.session_list_edit_help_body),
        )
    }
}

/** the "≡" a past session is held by, the caller passes the drag handle modifier */
@Composable
internal fun SessionHandle(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.session_list_edit_handle_description)
    IconBadge(
        modifier = modifier.semantics { contentDescription = description },
        size = 40.dp,
        shape = AppShapes.Field,
        background = AppPalette.SurfaceInput,
        border = AppPalette.Border,
    ) {
        Text("≡", style = MaterialTheme.typography.titleLarge, color = AppPalette.TextSecondary)
    }
}

@Composable
internal fun PastSessionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppDashedTile(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.session_list_edit_add_past),
            style = MaterialTheme.typography.labelLarge,
            color = AppPalette.GoldBright,
        )
    }
}

/** the dashed "between two sessions" tile that opens a new downtime */
@Composable
internal fun GapTile(
    upperLabel: String,
    lowerLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppDashedTile(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = AppPalette.Parchment.copy(alpha = 0.4f),
        cornerRadius = 10.dp,
        minHeight = 40.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            stringResource(Res.string.session_list_edit_between_format, lowerLabel, upperLabel),
            style = MaterialTheme.typography.labelLarge,
            color = AppPalette.Parchment,
        )
    }
}
