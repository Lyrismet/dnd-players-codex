package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppNameInput
import com.lyrismet.incadent.core.designsystem.component.AppSelectSheetContent
import com.lyrismet.incadent.core.designsystem.component.AppSelectTexts
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.feature.codex.domain.grouping.namesWithCustomType
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.CustomCreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import com.lyrismet.incadent.feature.codex.presentation.summary.creatureTypeLabels
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_delete_button
import dndplayerscodex.shared.generated.resources.species_type_create
import dndplayerscodex.shared.generated.resources.species_type_delete_confirm_text
import dndplayerscodex.shared.generated.resources.species_type_delete_confirm_title
import dndplayerscodex.shared.generated.resources.species_type_delete_confirm_unused
import dndplayerscodex.shared.generated.resources.species_type_edit_title
import dndplayerscodex.shared.generated.resources.species_type_empty
import dndplayerscodex.shared.generated.resources.species_type_group_builtin
import dndplayerscodex.shared.generated.resources.species_type_group_own
import dndplayerscodex.shared.generated.resources.species_type_name_placeholder
import dndplayerscodex.shared.generated.resources.species_type_none
import dndplayerscodex.shared.generated.resources.species_type_save
import dndplayerscodex.shared.generated.resources.species_type_search
import dndplayerscodex.shared.generated.resources.species_type_title
import dndplayerscodex.shared.generated.resources.species_type_unused
import dndplayerscodex.shared.generated.resources.species_type_used
import org.jetbrains.compose.resources.stringResource

/** the creature type sheet's content - a row id is negative for a built-in type and the record id for an own one */
data class CreatureTypeSelectState(
    val options: List<SelectOption>,
    val selectedIds: Set<Long>,
    val types: Map<Long, SpeciesType>,
    // the species that carry each own type, by its id
    val usedBy: Map<Long, List<String>>,
)

/** what the sheet asks of its host, the host owns the writes */
class CreatureTypeSheetActions(
    val onChosen: (SpeciesType) -> Unit,
    val onCreated: (String) -> Unit,
    val onRenamed: (id: Long, name: String) -> Unit,
    val onDeleted: (id: Long) -> Unit,
    val onDismiss: () -> Unit,
)

private fun CreatureType.optionId(): Long = -(ordinal + 1L)

/** the types to offer - the player's own first, then the built-in ones, the species' current one ticked */
@Composable
fun creatureTypeSelectState(
    current: SpeciesType,
    customTypes: List<CustomCreatureType>,
    species: List<Species>,
): CreatureTypeSelectState {
    val labels = creatureTypeLabels()
    val ownGroup = stringResource(Res.string.species_type_group_own)
    val builtinGroup = stringResource(Res.string.species_type_group_builtin)
    val builtin = CreatureType.entries.filter { it != CreatureType.UNSPECIFIED }
    val types =
        customTypes.associate { it.id to SpeciesType.Custom(it) } +
            builtin.associate { it.optionId() to SpeciesType.Builtin(it) }
    return CreatureTypeSelectState(
        options =
            customTypes.map { SelectOption(it.id, it.name, group = ownGroup, editable = true) } +
                builtin.map { SelectOption(it.optionId(), labels.getValue(it), group = builtinGroup) },
        selectedIds =
            when (current) {
                is SpeciesType.Builtin -> setOf(current.type.optionId())
                is SpeciesType.Custom -> setOf(current.type.id)
                SpeciesType.None -> emptySet()
            },
        types = types,
        usedBy = customTypes.associate { it.id to species.namesWithCustomType(it.id) },
    )
}

/** the creature type select over a form or a card, own types can be added, renamed and deleted right in it */
@Composable
fun CreatureTypeSelectSheet(
    select: CreatureTypeSelectState,
    actions: CreatureTypeSheetActions,
) {
    // the own type being edited, null while the list shows
    var editing by remember { mutableStateOf<Long?>(null) }
    val target = editing?.let { id -> select.options.find { it.id == id } }
    AppBottomSheet(onDismissRequest = actions.onDismiss) {
        if (target == null) {
            TypeList(select, actions) { editing = it }
        } else {
            TypeEdit(
                name = target.title,
                usedBy = select.usedBy[target.id].orEmpty(),
                takenNames = select.options.filter { it.editable && it.id != target.id }.map { it.title.lowercase() },
                onSaved = { name ->
                    actions.onRenamed(target.id, name)
                    editing = null
                },
                onDeleted = {
                    actions.onDeleted(target.id)
                    editing = null
                },
                onBack = { editing = null },
            )
        }
    }
}

@Composable
private fun TypeList(
    select: CreatureTypeSelectState,
    actions: CreatureTypeSheetActions,
    onEdit: (Long) -> Unit,
) {
    val texts =
        AppSelectTexts(
            title = stringResource(Res.string.species_type_title),
            searchPlaceholder = stringResource(Res.string.species_type_search),
            emptyText = stringResource(Res.string.species_type_empty),
            multiHint = "",
            clearLabel = stringResource(Res.string.species_type_none),
            doneLabel = { "" },
            createLabel = { query -> stringResource(Res.string.species_type_create, query) },
        )
    AppSelectSheetContent(
        texts = texts,
        options = select.options,
        selectedIds = select.selectedIds,
        multi = false,
        onConfirm = { ids -> actions.onChosen(ids.firstOrNull()?.let { select.types[it] } ?: SpeciesType.None) },
        onCreate = { name, _ -> actions.onCreated(name) },
        onEdit = onEdit,
    )
}

@Composable
private fun TypeEdit(
    name: String,
    usedBy: List<String>,
    takenNames: List<String>,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember(name) { mutableStateOf(name) }
    var confirmingDelete by remember { mutableStateOf(false) }
    AppSheetColumn(
        spacing = 14.dp,
        header = { AppSheetHeader(title = stringResource(Res.string.species_type_edit_title), onClose = onBack) },
    ) {
        AppNameInput(
            name = name,
            onNameChanged = { draft = it },
            placeholder = stringResource(Res.string.species_type_name_placeholder),
        )
        Text(
            text =
                if (usedBy.isEmpty()) {
                    stringResource(Res.string.species_type_unused)
                } else {
                    stringResource(Res.string.species_type_used, usedBy.joinToString(", "))
                },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.SmallPlus),
            color = AppPalette.TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FormPrimaryButton(
                text = stringResource(Res.string.species_type_save),
                onClick = { onSaved(draft) },
                enabled = draft.isNotBlank() && draft.trim().lowercase() !in takenNames,
            )
            AppButton(
                text = stringResource(Res.string.entity_sheet_delete_button),
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
            )
        }
    }
    if (confirmingDelete) {
        ConfirmationDialog(
            title = stringResource(Res.string.species_type_delete_confirm_title),
            text =
                if (usedBy.isEmpty()) {
                    stringResource(Res.string.species_type_delete_confirm_unused)
                } else {
                    stringResource(Res.string.species_type_delete_confirm_text, usedBy.joinToString(", "))
                },
            onConfirm = {
                confirmingDelete = false
                onDeleted()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}
