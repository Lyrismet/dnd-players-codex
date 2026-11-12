package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.repository.ItemRepository
import com.slack.circuit.retained.rememberRetained
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.items_owner_changed_title
import org.jetbrains.compose.resources.stringResource

/** the item sheet and the gear pick of the codex screen, kept across a push and pop of the navigation */
@Composable
internal fun rememberItemsController(
    repository: ItemRepository,
    undoController: UndoController,
): ItemsController {
    val draft = rememberRetained { mutableStateOf<ItemDraft?>(null) }
    val ownerPicker = rememberRetained { mutableStateOf(false) }
    val equipment = rememberRetained { mutableStateOf<EquipmentDraft?>(null) }
    val gearTitle = stringResource(Res.string.items_owner_changed_title)
    return remember(repository, undoController, gearTitle) {
        ItemsController(draft, ownerPicker, equipment, repository, undoController, ItemTexts(gearTitle))
    }
}
