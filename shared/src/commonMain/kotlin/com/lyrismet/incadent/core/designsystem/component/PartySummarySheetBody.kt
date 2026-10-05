package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem.PartySummary
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_party_presence_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_type_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PartySummaryBody(
    party: PartySummary,
    actions: EntitySummarySheetActions,
    onClose: () -> Unit,
) {
    EntityHeaderRow(
        emblem = party.emblem,
        overlineTypeLabel = stringResource(Res.string.entity_sheet_party_type_label),
        overlineValue = party.overlineValue,
        title = party.name,
        subtitle = party.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(party.ref) } },
        onClose = onClose,
    )
    actions.onPartyPresenceSelected?.let { onSelected ->
        EntityStatusSection(
            title = stringResource(Res.string.entity_sheet_party_presence_label),
            options = party.presenceOptions,
            onSelected = { presence -> onSelected(party.ref.id, presence) },
        )
    }
    if (party.description.isNotBlank()) {
        Text(party.description, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextDescription)
    }
    EntityFactsGrid(party.facts, actions.onEntityRefClicked)
}
