package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSelectSheetContent
import com.lyrismet.incadent.core.designsystem.component.AppSelectTexts
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_species
import dndplayerscodex.shared.generated.resources.codex_place_select_done
import dndplayerscodex.shared.generated.resources.codex_place_select_empty
import dndplayerscodex.shared.generated.resources.codex_place_select_multi_hint
import dndplayerscodex.shared.generated.resources.codex_species_select_clear
import dndplayerscodex.shared.generated.resources.codex_species_select_create
import dndplayerscodex.shared.generated.resources.codex_species_select_search
import org.jetbrains.compose.resources.stringResource

/** the species select as a sheet over the npc form, an empty answer clears the link */
@Composable
internal fun SpeciesSelectSheet(
    select: SpeciesSelectState,
    onChosen: (List<Long>) -> Unit,
    onSrdChosen: (SrdSuggestion) -> Unit,
    onCreated: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val texts =
        AppSelectTexts(
            title = stringResource(Res.string.codex_entry_label_species),
            searchPlaceholder = stringResource(Res.string.codex_species_select_search),
            emptyText = stringResource(Res.string.codex_place_select_empty),
            multiHint = stringResource(Res.string.codex_place_select_multi_hint),
            clearLabel = stringResource(Res.string.codex_species_select_clear),
            doneLabel = { count -> stringResource(Res.string.codex_place_select_done, count) },
            createLabel = { query -> stringResource(Res.string.codex_species_select_create, query) },
        )
    AppBottomSheet(onDismissRequest = onDismiss) {
        AppSelectSheetContent(
            texts = texts,
            options = select.options,
            selectedIds = select.selectedIds,
            multi = false,
            onConfirm = { ids ->
                val srd = ids.firstNotNullOfOrNull { select.srd[it] }
                if (srd != null) onSrdChosen(srd) else onChosen(ids.toList())
            },
            leadingGlyph = MentionGlyph.SPECIES.symbol,
            onCreate = { name, _ -> onCreated(name) },
        )
    }
}
