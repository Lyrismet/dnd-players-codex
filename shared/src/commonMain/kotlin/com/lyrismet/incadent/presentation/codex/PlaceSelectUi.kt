package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSelectSheetContent
import com.lyrismet.incadent.core.designsystem.component.AppSelectTexts
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_place_select_clear
import dndplayerscodex.shared.generated.resources.codex_place_select_done
import dndplayerscodex.shared.generated.resources.codex_place_select_empty
import dndplayerscodex.shared.generated.resources.codex_place_select_multi_hint
import dndplayerscodex.shared.generated.resources.codex_place_select_search
import dndplayerscodex.shared.generated.resources.codex_search_create_location
import org.jetbrains.compose.resources.stringResource

/** the place select as a sheet of its own, shown over whichever sheet opened it */
@Composable
internal fun PlaceSelectSheet(
    select: PlaceSelectState,
    onChosen: (List<Long>) -> Unit,
    onCreated: (name: String, checked: List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        PlaceSelectContent(select = select, onChosen = onChosen, onCreated = onCreated)
    }
}

/** the place select sheet's body - the form and the card's quick edit both host it and answer through the callbacks */
@Composable
internal fun PlaceSelectContent(
    select: PlaceSelectState,
    onChosen: (List<Long>) -> Unit,
    onCreated: (name: String, checked: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts =
        AppSelectTexts(
            title = select.title,
            searchPlaceholder = stringResource(Res.string.codex_place_select_search),
            emptyText = stringResource(Res.string.codex_place_select_empty),
            multiHint = stringResource(Res.string.codex_place_select_multi_hint),
            clearLabel = stringResource(Res.string.codex_place_select_clear),
            doneLabel = { count -> stringResource(Res.string.codex_place_select_done, count) },
            createLabel = { query -> stringResource(Res.string.codex_search_create_location, query) },
        )
    AppSelectSheetContent(
        texts = texts,
        options = select.options,
        selectedIds = select.selectedIds,
        multi = select.multi,
        onConfirm = { ids -> onChosen(ids.toList()) },
        modifier = modifier,
        recentIds = select.recentIds,
        excludedIds = select.excludedIds,
        leadingGlyph = MentionGlyph.LOCATION.symbol,
        onCreate = { name, checked -> onCreated(name, checked.toList()) },
    )
}
