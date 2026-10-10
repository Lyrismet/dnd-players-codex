package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
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
    EntityHeader(
        emblem = party.emblem,
        overlineTypeLabel = stringResource(Res.string.entity_sheet_party_type_label),
        overlineValue = party.overlineValue,
        title = party.name,
        subtitle = party.subtitle,
        onEditClicked = actions.onEditClicked?.let { edit -> { edit(party.ref) } },
        onClose = onClose,
        quick = actions.quickEdit,
    )
    actions.onPartyPresenceSelected?.let { onSelected ->
        val title = stringResource(Res.string.entity_sheet_party_presence_label)
        if (actions.statusesReadOnly) {
            party.presenceOptions.firstOrNull { it.isSelected }?.let {
                EntityStatusSeals(
                    listOf(StatusSeal(title, it)),
                )
            }
        } else {
            EntityStatusSection(
                title = title,
                options = party.presenceOptions,
                onSelected = { presence -> onSelected(party.ref.id, presence) },
            )
        }
    }
    EntityDescription(party.description, actions.quickEdit)
    EntityFactsGrid(party.facts, actions.onEntityRefClicked, actions.quickEdit)
}
