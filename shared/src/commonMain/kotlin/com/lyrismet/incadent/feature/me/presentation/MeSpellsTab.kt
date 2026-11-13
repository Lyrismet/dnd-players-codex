package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.feature.me.domain.isDown
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsBlock
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_edit_slots
import incadent.shared.generated.resources.me_spells_empty_action
import incadent.shared.generated.resources.me_spells_empty_body
import incadent.shared.generated.resources.me_spells_empty_title
import incadent.shared.generated.resources.me_spells_hide_action
import incadent.shared.generated.resources.me_spells_hide_hint
import org.jetbrains.compose.resources.stringResource

/** places the shared spells block of the character with the buttons that change the slots */
@Composable
internal fun MeSpellsTab(
    character: MeCharacter,
    listState: LazyListState,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val send = { event: CounterEvent -> onEvent(MeEvent.Counters(event)) }
    val spells = character.spells
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (character.vitals.isDown) item { MeDeathSaves(character, onEvent) }
        item { SpellsBlock(spells, { onEvent(MeEvent.Spells(it)) }) }
        if (spells.columns.isEmpty()) {
            item {
                EmptySlots(
                    character.readOnly,
                    onEdit = { send(CounterEvent.SlotsRequested) },
                    onHide = { send(CounterEvent.SpellsHidden) },
                )
            }
        } else if (!character.readOnly) {
            item {
                AppButton(
                    text = stringResource(Res.string.me_spells_edit_slots),
                    onClick = { send(CounterEvent.SlotsRequested) },
                    modifier = Modifier.fillMaxWidth(),
                    tone = ButtonTone.Neutral,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun EmptySlots(
    readOnly: Boolean,
    onEdit: () -> Unit,
    onHide: () -> Unit,
) {
    MeEmptyState(
        stringResource(Res.string.me_spells_empty_title),
        stringResource(Res.string.me_spells_empty_body),
        Modifier.padding(12.dp),
        spacing = 12.dp,
    ) {
        if (!readOnly) {
            AppButton(
                text = stringResource(Res.string.me_spells_empty_action),
                onClick = onEdit,
                variant = ButtonVariant.Solid,
                height = 48.dp,
            )
            AppButton(
                text = stringResource(Res.string.me_spells_hide_action),
                onClick = onHide,
                tone = ButtonTone.Neutral,
                variant = ButtonVariant.Ghost,
                height = 48.dp,
            )
            Text(
                stringResource(Res.string.me_spells_hide_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
