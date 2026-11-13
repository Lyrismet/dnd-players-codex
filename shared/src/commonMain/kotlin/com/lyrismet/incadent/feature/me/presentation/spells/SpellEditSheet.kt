package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_edit
import incadent.shared.generated.resources.me_spells_edit_add
import incadent.shared.generated.resources.me_spells_edit_duration
import incadent.shared.generated.resources.me_spells_edit_name
import incadent.shared.generated.resources.me_spells_edit_name_hint
import incadent.shared.generated.resources.me_spells_edit_new
import incadent.shared.generated.resources.me_spells_edit_overline
import incadent.shared.generated.resources.me_spells_edit_range
import incadent.shared.generated.resources.me_spells_edit_text
import incadent.shared.generated.resources.me_spells_edit_time
import incadent.shared.generated.resources.me_spells_edit_title
import org.jetbrains.compose.resources.stringResource

/** the editor of one spell: the fields in the order of the mockup, a copy of the spell goes up on every change */
@Composable
internal fun SpellEditSheet(
    spell: CharacterSpell,
    send: (SpellsEvent) -> Unit,
) {
    val close = { send(SpellsEvent.DraftClosed) }
    val change = { next: CharacterSpell -> send(SpellsEvent.DraftChanged(next)) }
    val isNew = spell.id == 0L
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title =
                        stringResource(
                            if (isNew) Res.string.me_spells_edit_new else Res.string.me_spells_edit_title,
                        ),
                    onClose = close,
                    overline = stringResource(Res.string.me_spells_edit_overline),
                )
            },
        ) {
            FormTextField(
                label = stringResource(Res.string.me_spells_edit_name),
                value = spell.name,
                onChange = { change(spell.copy(name = it)) },
                placeholder = stringResource(Res.string.me_spells_edit_name_hint),
                required = true,
            )
            LevelTiles(spell.level) { change(spell.copy(level = it)) }
            SchoolField(spell.school) { change(spell.copy(school = it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    stringResource(Res.string.me_spells_edit_time),
                    spell.castingTime,
                    { change(spell.copy(castingTime = it)) },
                    Modifier.weight(1f),
                )
                FormTextField(
                    stringResource(Res.string.me_spells_edit_range),
                    spell.range,
                    { change(spell.copy(range = it)) },
                    Modifier.weight(1f),
                )
            }
            ComponentFields(spell, change)
            FormTextField(
                stringResource(Res.string.me_spells_edit_duration),
                spell.duration,
                { change(spell.copy(duration = it)) },
            )
            FlagCards(spell, change)
            FormTextArea(stringResource(Res.string.me_spells_edit_text), spell.text, { change(spell.copy(text = it)) })
            FormPrimaryButton(
                text = stringResource(if (isNew) Res.string.me_spells_edit_add else Res.string.me_spells_card_edit),
                onClick = { send(SpellsEvent.DraftSaved) },
                enabled = spell.name.isNotBlank(),
            )
        }
    }
}
