package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.format.kindLabelRes
import com.lyrismet.incadent.domain.model.EntityKind
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_type_species
import dndplayerscodex.shared.generated.resources.codex_party_fallen_word
import dndplayerscodex.shared.generated.resources.entity_sheet_overline_format
import dndplayerscodex.shared.generated.resources.entity_sheet_party_type_label
import org.jetbrains.compose.resources.stringResource

/** the type, status and the edit and close buttons of a card - they stay above the scrolling body */
@Composable
internal fun PinnedOverline(
    item: EntitySummaryItem,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    val parts =
        when (item) {
            is EntitySummaryItem.NpcSummary ->
                OverlineParts(
                    stringResource(EntityKind.NPC.kindLabelRes()),
                    item.statusLabel,
                    item.emblem,
                    item.ref,
                )
            is EntitySummaryItem.LocationSummary ->
                OverlineParts(
                    stringResource(EntityKind.LOCATION.kindLabelRes()),
                    "",
                    item.emblem,
                    item.ref,
                )
            is EntitySummaryItem.QuestSummary ->
                OverlineParts(
                    stringResource(EntityKind.QUEST.kindLabelRes()),
                    item.statusLabel,
                    item.emblem,
                    item.ref,
                )
            is EntitySummaryItem.PartySummary ->
                OverlineParts(
                    stringResource(Res.string.entity_sheet_party_type_label),
                    if (item.fall != null) stringResource(Res.string.codex_party_fallen_word) else item.overlineValue,
                    item.emblem,
                    item.ref,
                )
            is EntitySummaryItem.SpeciesSummary ->
                OverlineParts(
                    stringResource(Res.string.codex_entry_type_species),
                    creatureTypeLabels().getValue(item.creatureType),
                    item.emblem,
                    item.ref,
                )
        }
    EntityOverlineRow(
        overline =
            if (parts.value.isBlank()) {
                parts.typeLabel.uppercase()
            } else {
                stringResource(
                    Res.string.entity_sheet_overline_format,
                    parts.typeLabel.uppercase(),
                    parts.value.uppercase(),
                )
            },
        color = parts.emblem.color.foreground,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(parts.ref) } },
        onClose = onClose,
    )
}

private class OverlineParts(
    val typeLabel: String,
    val value: String,
    val emblem: EntityEmblem,
    val ref: EntityRef,
)

private val HeaderButtonSize = 36.dp

@Composable
private fun EntityOverlineRow(
    overline: String,
    color: Color,
    onEditClicked: (() -> Unit)?,
    onClose: () -> Unit,
) {
    // already uppercased by the caller's string resource, so SectionOverline's own uppercase is a no-op here
    SectionOverline(
        text = overline,
        color = color,
        style = OverlineStyle.Section,
        trailingContent = {
            // matches the original Row's spacedBy(8.dp), now that the overline and the buttons are split apart
            Spacer(Modifier.width(8.dp))
            onEditClicked?.let { onClick ->
                IconBadge(
                    size = HeaderButtonSize,
                    background = AppPalette.Gold.copy(alpha = 0.12f),
                    border = AppPalette.Gold.copy(alpha = 0.45f),
                    onClick = onClick,
                ) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = null,
                        tint = AppPalette.GoldBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            SheetCloseButton(onClick = onClose, size = HeaderButtonSize)
        },
    )
}
