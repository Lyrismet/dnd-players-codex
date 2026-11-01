package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppNameInput
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon
import com.lyrismet.incadent.core.designsystem.component.icons.entity.icon
import com.lyrismet.incadent.core.format.kindLabelRes
import com.lyrismet.incadent.domain.model.EntityKind
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.mention_create_button
import dndplayerscodex.shared.generated.resources.mention_create_button_empty
import dndplayerscodex.shared.generated.resources.mention_create_button_unlinked
import dndplayerscodex.shared.generated.resources.mention_create_footer
import dndplayerscodex.shared.generated.resources.mention_create_link_hint
import dndplayerscodex.shared.generated.resources.mention_create_name_label
import dndplayerscodex.shared.generated.resources.mention_create_name_placeholder
import dndplayerscodex.shared.generated.resources.mention_create_overline
import dndplayerscodex.shared.generated.resources.mention_create_title
import dndplayerscodex.shared.generated.resources.mention_create_type_label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** names and types a record before it is mentioned - the name can still be finished here */
@Composable
internal fun MentionCreateSheet(
    sheet: MentionCreateSheetState,
    actions: MentionCreateActions,
) {
    val dismiss = actions.onDismiss
    AppBottomSheet(onDismissRequest = dismiss) {
        AppSheetColumn(spacing = 16.dp, scroll = true, header = {
            AppSheetHeader(
                overline = stringResource(Res.string.mention_create_overline),
                title = stringResource(Res.string.mention_create_title),
                onClose = dismiss,
            )
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormFieldLabel(stringResource(Res.string.mention_create_name_label))
                val namePlaceholder = stringResource(Res.string.mention_create_name_placeholder)
                AppNameInput(sheet.name, actions.onNameChanged, namePlaceholder)
                val hint =
                    sheet.linkedNotes?.let {
                        pluralStringResource(
                            Res.plurals.mention_create_link_hint,
                            it,
                            it,
                            sheet.name.trim(),
                        )
                    }
                        ?: stringResource(Res.string.mention_create_footer)
                Text(hint, fontSize = AppFontSize.SmallPlus, color = AppPalette.TextSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormFieldLabel(stringResource(Res.string.mention_create_type_label))
                sheet.kinds.chunked(TYPES_PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { kind ->
                            MentionTypeTile(
                                kind = kind,
                                selected = kind == sheet.type,
                                modifier = Modifier.weight(1f),
                                onClick = { actions.onTypeChanged(kind) },
                            )
                        }
                        repeat(TYPES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            FormPrimaryButton(
                text = stringResource(sheet.buttonRes()),
                onClick = actions.onConfirm,
                enabled = sheet.canCreate,
            )
            // the tag sheet keeps its note under the button, the draft one has it under the field
            if (sheet.linkedNotes != null) {
                Text(
                    stringResource(Res.string.mention_create_footer),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = AppFontSize.SmallPlus,
                    color = AppPalette.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val TYPES_PER_ROW = 5

private fun MentionCreateSheetState.buttonRes(): StringResource =
    when {
        !canCreate -> Res.string.mention_create_button_empty
        linkedNotes != null -> Res.string.mention_create_button_unlinked
        else -> Res.string.mention_create_button
    }

// the glyph over the label, the picked one framed in gold
@Composable
private fun MentionTypeTile(
    kind: EntityKind,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val frame = if (selected) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.Border
    val fill = if (selected) AppPalette.SurfaceElevated else Color.Transparent
    Column(
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .clip(AppShapes.Field)
                .background(fill)
                .border(1.dp, frame, AppShapes.Field)
                .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.5.dp, Alignment.CenterVertically),
    ) {
        AppEntityIcon(
            kind.icon(),
            if (selected) AppPalette.GoldBright else AppPalette.TextSecondary,
            size = 20.dp,
        )
        Text(
            stringResource(kind.kindLabelRes()),
            fontSize = AppFontSize.Body,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) AppPalette.TextHeading else AppPalette.TextMuted,
        )
    }
}

/** the record kinds the create sheet offers */
internal val MentionCreateKinds =
    listOf(EntityKind.NPC, EntityKind.QUEST, EntityKind.LOCATION, EntityKind.PARTY, EntityKind.SPECIES)
