package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.presentation.form.PlaceSelectSheet
import com.lyrismet.incadent.feature.codex.presentation.form.SpeciesSelectSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummarySheetActions
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummarySheetContent

/** the entity card as a bottom sheet with the select sheets its quick edits open over it, every screen shows it so */
@Composable
fun EntityCardSheet(
    item: EntitySummaryItem,
    actions: EntitySummarySheetActions,
    quick: QuickEditState,
    onQuickEvent: (QuickEditUiEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Box {
            EntitySummarySheetContent(item = item, actions = actions, onClose = onDismiss)
            UndoToast(
                action = quick.undoAction,
                onUndo = { onQuickEvent(QuickEditUiEvent.UndoClicked) },
                onFollowUp = { onQuickEvent(QuickEditUiEvent.UndoFollowUpClicked) },
                modifier = floatingBarAnchor(),
            )
        }
    }
    // sheets of their own over the card, so dismissing one leaves the card open
    quick.placeSelect?.let { select ->
        PlaceSelectSheet(
            select = select,
            onChosen = { onQuickEvent(QuickEditUiEvent.PlacesChosen(it)) },
            onCreated = { name, checked -> onQuickEvent(QuickEditUiEvent.PlaceCreated(name, checked)) },
            onDismiss = { onQuickEvent(QuickEditUiEvent.EditCancelled) },
        )
    }
    quick.speciesSelect?.let { select ->
        SpeciesSelectSheet(
            select = select,
            onChosen = { onQuickEvent(QuickEditUiEvent.LinkChosen(it.firstOrNull())) },
            onSrdChosen = { onQuickEvent(QuickEditUiEvent.SpeciesCreated(it.name, it.type)) },
            onCreated = { onQuickEvent(QuickEditUiEvent.SpeciesCreated(it, CreatureType.HUMANOID)) },
            onDismiss = { onQuickEvent(QuickEditUiEvent.EditCancelled) },
        )
    }
}
