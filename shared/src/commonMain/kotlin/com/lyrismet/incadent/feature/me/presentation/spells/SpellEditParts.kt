package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppToggleCard
import com.lyrismet.incadent.core.designsystem.component.FormFieldHeight
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_LEVEL
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_cantrip
import incadent.shared.generated.resources.me_spells_component_m
import incadent.shared.generated.resources.me_spells_component_s
import incadent.shared.generated.resources.me_spells_component_v
import incadent.shared.generated.resources.me_spells_edit_components
import incadent.shared.generated.resources.me_spells_edit_concentration
import incadent.shared.generated.resources.me_spells_edit_level
import incadent.shared.generated.resources.me_spells_edit_material
import incadent.shared.generated.resources.me_spells_edit_ritual
import incadent.shared.generated.resources.me_spells_edit_school
import incadent.shared.generated.resources.me_spells_school_abjuration
import incadent.shared.generated.resources.me_spells_school_conjuration
import incadent.shared.generated.resources.me_spells_school_divination
import incadent.shared.generated.resources.me_spells_school_enchantment
import incadent.shared.generated.resources.me_spells_school_evocation
import incadent.shared.generated.resources.me_spells_school_illusion
import incadent.shared.generated.resources.me_spells_school_necromancy
import incadent.shared.generated.resources.me_spells_school_none
import incadent.shared.generated.resources.me_spells_school_title
import incadent.shared.generated.resources.me_spells_school_transmutation
import org.jetbrains.compose.resources.stringResource

private const val LEVEL_TILES_IN_ROW = 5
private val TileHeight = 40.dp

/** the cantrip and the nine levels as tiles in two rows, one press picks it so a high level is not many presses */
@Composable
internal fun LevelTiles(
    level: Int,
    onPick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_level))
        Column(
            Modifier
                .fillMaxWidth()
                .appCard(
                    AppShapes.Field,
                    AppPalette.Background,
                    AppPalette.BorderSubtle,
                ).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (listOf(CANTRIP_LEVEL) + SPELL_LEVELS.toList()).chunked(LEVEL_TILES_IN_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { value ->
                        val on = value == level
                        Box(
                            Modifier
                                .weight(1f)
                                .height(TileHeight)
                                .background(
                                    if (on) AppPalette.SurfaceElevated else AppPalette.Background,
                                    AppShapes.Tile,
                                ).clickable { onPick(value) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (value ==
                                    CANTRIP_LEVEL
                                ) {
                                    stringResource(Res.string.me_spells_card_cantrip)
                                } else {
                                    value.toRomanNumeral()
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (on) AppPalette.GoldBright else AppPalette.TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** the school shows as a field that opens a list sheet, an empty one reads "not chosen" */
@Composable
internal fun SchoolField(
    school: String,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    FormPickField(
        label = stringResource(Res.string.me_spells_edit_school),
        placeholder = stringResource(Res.string.me_spells_school_none),
        onClick = { open = true },
        title = school.ifBlank { null },
    )
    if (open) {
        SchoolSheet(school, { open = false }) {
            onPick(it)
            open = false
        }
    }
}

@Composable
private fun SchoolSheet(
    chosen: String,
    onClose: () -> Unit,
    onPick: (String) -> Unit,
) {
    val none = stringResource(Res.string.me_spells_school_none)
    val schools =
        listOf(
            stringResource(Res.string.me_spells_school_abjuration),
            stringResource(Res.string.me_spells_school_conjuration),
            stringResource(Res.string.me_spells_school_divination),
            stringResource(Res.string.me_spells_school_enchantment),
            stringResource(Res.string.me_spells_school_evocation),
            stringResource(Res.string.me_spells_school_illusion),
            stringResource(Res.string.me_spells_school_necromancy),
            stringResource(Res.string.me_spells_school_transmutation),
        )
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            scroll = true,
            header = { AppSheetHeader(title = stringResource(Res.string.me_spells_school_title), onClose = onClose) },
        ) {
            (listOf("" to none) + schools.map { it to it }).forEach { (value, label) ->
                val on = value == chosen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .appCard(
                            AppShapes.Field,
                            if (on) AppPalette.SurfaceElevated else AppPalette.Background,
                            if (on) AppPalette.Gold else AppPalette.BorderSubtle,
                            onClick = { onPick(value) },
                        ).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (on) AppPalette.GoldBright else AppPalette.TextPrimary,
                    )
                }
            }
        }
    }
}

/** the three letters as square toggles and the material beside them, which only takes text while the M is on */
@Composable
internal fun ComponentFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_components))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterToggle(
                stringResource(Res.string.me_spells_component_v),
                spell.verbal,
            ) { change(spell.copy(verbal = it)) }
            LetterToggle(
                stringResource(Res.string.me_spells_component_s),
                spell.somatic,
            ) { change(spell.copy(somatic = it)) }
            LetterToggle(stringResource(Res.string.me_spells_component_m), spell.hasMaterial) {
                change(spell.copy(hasMaterial = it))
            }
            FormInlineField(
                value = spell.material,
                onChange = { change(spell.copy(material = it)) },
                modifier = Modifier.weight(1f),
                placeholder = stringResource(Res.string.me_spells_edit_material),
                enabled = spell.hasMaterial,
            )
        }
    }
}

@Composable
private fun LetterToggle(
    text: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Box(
        Modifier
            .size(FormFieldHeight)
            .appCard(
                AppShapes.Field,
                if (on) AppPalette.Arcane.copy(alpha = 0.12f) else AppPalette.Background,
                if (on) AppPalette.Arcane else AppPalette.BorderSubtle,
                onClick = { onChange(!on) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (on) AppPalette.Arcane else AppPalette.TextTertiary,
        )
    }
}

/** concentration and ritual as two cards with a switch, one under the other so the name stays on one line */
@Composable
internal fun FlagCards(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppToggleCard(
            "◎ " + stringResource(Res.string.me_spells_edit_concentration),
            null,
            spell.concentration,
            { change(spell.copy(concentration = !spell.concentration)) },
            Modifier.heightIn(min = 56.dp),
        )
        AppToggleCard(
            stringResource(Res.string.me_spells_edit_ritual),
            null,
            spell.ritual,
            { change(spell.copy(ritual = !spell.ritual)) },
            Modifier.heightIn(min = 56.dp),
        )
    }
}
