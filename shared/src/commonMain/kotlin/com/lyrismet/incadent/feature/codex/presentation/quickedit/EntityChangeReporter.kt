package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.undo.UndoController
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.place_move_from
import dndplayerscodex.shared.generated.resources.place_move_here
import dndplayerscodex.shared.generated.resources.quick_edit_changed_suffix
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.stringResource

/** offers every committed change of a card to the undo bar, a place move is titled by the move itself */
@Composable
fun rememberEntityChangeReporter(undoController: UndoController): suspend (EntityChange) -> Unit {
    val fieldTitles = rememberQuickEditFieldTitles()
    val changedSuffix = stringResource(Res.string.quick_edit_changed_suffix)
    return { change ->
        val title = fieldTitles.getValue(change.field)
        val shown = if (change.field == QuickEditField.MOVE) title else title + changedSuffix
        undoController.show(shown, change.subtitle(), change.restore)
    }
}

// the places a change moved are told as a sentence, any other change is told by the name of its record
private suspend fun EntityChange.subtitle(): String {
    val places = moved?.takeIf { it.count > 0 } ?: return entityName
    return if (places.fromName != null) {
        getPluralString(Res.plurals.place_move_from, places.count, places.count, places.fromName)
    } else {
        getPluralString(Res.plurals.place_move_here, places.count, places.count)
    }
}
