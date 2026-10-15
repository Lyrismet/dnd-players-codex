package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.quickedit.RelationOptionEditor
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.RelationGroup
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.quick_edit_relations_hint
import org.jetbrains.compose.resources.stringResource

/** the titled chip rows under the facts - a group with an edit field opens its relation editor on a hold */
@Composable
internal fun EntityRelationGroups(
    groups: List<RelationGroup>,
    onEntityRefClicked: (EntityRef) -> Unit,
    quick: QuickEditSheet?,
    modifier: Modifier = Modifier,
) {
    // a group with nothing in it only stays while it can be filled by holding it
    val shown = groups.filter { it.items.isNotEmpty() || (quick != null && it.edit != null) }
    if (shown.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        shown.forEach { group ->
            val field = group.edit
            val title = stringResource(group.title)
            when {
                quick == null || field == null -> RelationGroupContent(group, title, onEntityRefClicked)
                quick.inlineEdit?.field == field ->
                    RelationOptionEditor(
                        label = title,
                        options = group.options,
                        hint = stringResource(Res.string.quick_edit_relations_hint),
                        onEvent = quick.onEvent,
                    )
                else ->
                    HoldToEditField(
                        onHeld = { quick.onEvent(QuickEditUiEvent.FieldHeld(field, "")) },
                        frame = HoldFrame.Description,
                    ) {
                        RelationGroupContent(group, title, onEntityRefClicked)
                    }
            }
        }
    }
}

@Composable
private fun RelationGroupContent(
    group: RelationGroup,
    title: String,
    onEntityRefClicked: (EntityRef) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(text = title, fontSize = AppFontSize.Caption)
        if (group.items.isEmpty()) {
            group.emptyText?.let {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Body),
                    color = AppPalette.TextTertiary,
                )
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                group.items.forEach { chip ->
                    MentionChip(item = chip, onClick = chip.entityRef?.let { ref -> { onEntityRefClicked(ref) } })
                }
            }
        }
    }
}
