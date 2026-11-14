package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_edit_range
import incadent.shared.generated.resources.me_spells_other
import incadent.shared.generated.resources.me_spells_range_near
import incadent.shared.generated.resources.me_spells_range_own_hint
import incadent.shared.generated.resources.me_spells_range_self
import org.jetbrains.compose.resources.stringResource

/** the common ranges as chips and an own text, the chosen words are the range itself */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RangeFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    val common =
        listOf(stringResource(Res.string.me_spells_range_near), stringResource(Res.string.me_spells_range_self))
    var own by remember { mutableStateOf(spell.range.isNotBlank() && spell.range !in common) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_range))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            common.forEach { text ->
                SelectablePill(text, !own && spell.range == text, {
                    own = false
                    change(spell.copy(range = text))
                }, size = PillSize.Small)
            }
            SelectablePill(stringResource(Res.string.me_spells_other), own, { own = true }, size = PillSize.Small)
        }
        if (own) {
            FormInlineField(
                value = spell.range.takeIf { it !in common }.orEmpty(),
                onChange = { change(spell.copy(range = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.me_spells_range_own_hint),
            )
        }
    }
}
