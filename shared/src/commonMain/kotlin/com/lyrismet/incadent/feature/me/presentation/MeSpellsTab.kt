package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppCells
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
import com.lyrismet.incadent.core.designsystem.component.AppTriangle
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.CellSize
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.SheetAdjustment
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.isDown
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_help_slots_body
import incadent.shared.generated.resources.me_help_slots_title
import incadent.shared.generated.resources.me_help_spell_ability_body
import incadent.shared.generated.resources.me_help_spell_ability_title
import incadent.shared.generated.resources.me_help_spell_attack_body
import incadent.shared.generated.resources.me_help_spell_attack_title
import incadent.shared.generated.resources.me_help_spell_dc_body
import incadent.shared.generated.resources.me_help_spell_dc_title
import incadent.shared.generated.resources.me_slots_cell_hint
import incadent.shared.generated.resources.me_slots_free
import incadent.shared.generated.resources.me_slots_level_title
import incadent.shared.generated.resources.me_slots_list_placeholder
import incadent.shared.generated.resources.me_spells_edit_slots
import incadent.shared.generated.resources.me_spells_empty_action
import incadent.shared.generated.resources.me_spells_empty_body
import incadent.shared.generated.resources.me_spells_empty_title
import incadent.shared.generated.resources.me_spells_hide_action
import incadent.shared.generated.resources.me_spells_hide_hint
import incadent.shared.generated.resources.me_spells_slots_title
import incadent.shared.generated.resources.me_spells_tile_ability
import incadent.shared.generated.resources.me_spells_tile_attack
import incadent.shared.generated.resources.me_spells_tile_dc
import org.jetbrains.compose.resources.stringResource

/** draws the spellcasting numbers and the slots of the character, nothing about how they are stored */
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
        item { SpellTiles(spells, character.readOnly, send) }
        if (spells.columns.isEmpty()) {
            item {
                EmptySlots(
                    character.readOnly,
                    onEdit = { send(CounterEvent.SlotsRequested) },
                    onHide = { send(CounterEvent.SpellsHidden) },
                )
            }
        } else {
            item { SlotsHeader() }
            item { SlotRows(spells.columns, spells.openLevel, character.readOnly, send) }
            if (!character.readOnly) {
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
}

@Composable
private fun SpellTiles(
    spells: MeSpellsState,
    readOnly: Boolean,
    send: (CounterEvent) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SpellTile(
            value = spells.ability?.let { stringResource(it.abbrRes()) }.orDash(),
            label = stringResource(Res.string.me_spells_tile_ability),
            help =
                stringResource(Res.string.me_help_spell_ability_title) to
                    stringResource(Res.string.me_help_spell_ability_body),
            modifier = Modifier.weight(1f),
            onClick = if (readOnly) null else ({ send(CounterEvent.SpellAbilityRequested) }),
            dropdown = !readOnly,
        )
        SpellTile(
            value = spells.saveDc?.toString().orDash(),
            label = stringResource(Res.string.me_spells_tile_dc),
            help =
                stringResource(
                    Res.string.me_help_spell_dc_title,
                ) to stringResource(Res.string.me_help_spell_dc_body),
            modifier = Modifier.weight(1f),
            onClick = adjustClick(readOnly, spells.saveDc, SheetAdjustment.SPELL_DC, send),
        )
        SpellTile(
            value = spells.attack.signed(),
            label = stringResource(Res.string.me_spells_tile_attack),
            help =
                stringResource(Res.string.me_help_spell_attack_title) to
                    stringResource(Res.string.me_help_spell_attack_body),
            modifier = Modifier.weight(1f),
            onClick = adjustClick(readOnly, spells.attack, SheetAdjustment.SPELL_ATTACK, send),
        )
    }
}

// a number that is not calculated yet (no spell ability) has nothing to adjust
private fun adjustClick(
    readOnly: Boolean,
    value: Int?,
    target: SheetAdjustment,
    send: (CounterEvent) -> Unit,
): (() -> Unit)? = if (readOnly || value == null) null else ({ send(CounterEvent.SpellAdjustmentRequested(target)) })

@Composable
private fun SpellTile(
    value: String,
    label: String,
    help: Pair<String, String>,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
    dropdown: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .heightIn(min = 72.dp)
                .appCard(shape = AppShapes.Card, onClick = onClick)
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = AppText.numeral(20.sp), color = AppPalette.TextHeading)
            if (dropdown) AppTriangle(pointsUp = false, color = AppPalette.TextHeading, modifier = Modifier.size(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f, fill = false),
                style = AppText.caption,
                color = AppPalette.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            AppHelpHint(help.first, help.second, modifier = Modifier.size(HINT_SLOT))
        }
    }
}

private val HINT_SLOT = 28.dp

@Composable
private fun SlotsHeader() {
    MeSectionTitle(
        stringResource(Res.string.me_spells_slots_title),
        helpTitle = stringResource(Res.string.me_help_slots_title),
        helpBody = stringResource(Res.string.me_help_slots_body),
    )
}

private const val SLOT_TILES_IN_ROW = 3

@Composable
private fun SlotRows(
    columns: List<SlotColumn>,
    openLevel: Int?,
    readOnly: Boolean,
    send: (CounterEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val rows = remember(columns) { columns.chunked(SLOT_TILES_IN_ROW) }
        rows.forEach { SlotRow(it, openLevel, readOnly, send) }
    }
}

/** a row of up to three level tiles, the open level shows its cells in a panel under the row */
@Composable
private fun SlotRow(
    row: List<SlotColumn>,
    openLevel: Int?,
    readOnly: Boolean,
    send: (CounterEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { column ->
                SlotTile(column, column.level == openLevel, Modifier.weight(1f)) {
                    send(CounterEvent.SlotLevelToggled(column.level))
                }
            }
            repeat(SLOT_TILES_IN_ROW - row.size) { Spacer(Modifier.weight(1f)) }
        }
        row.firstOrNull { it.level == openLevel }?.let { SlotPanel(it, readOnly, send) }
    }
}

@Composable
private fun SlotTile(
    column: SlotColumn,
    open: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val resource = column.resource
    val hasFree = resource.current > 0
    Column(
        modifier =
            modifier
                .heightIn(min = 68.dp)
                .appCard(
                    background = if (open) AppPalette.SurfacePopover else AppPalette.Surface,
                    border = if (open) AppPalette.Arcane else AppPalette.BorderSubtle,
                    borderWidth = if (open) 1.5.dp else 1.dp,
                    onClick = onClick,
                ).padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                column.level.toRomanNumeral(),
                modifier = Modifier.weight(1f).alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (hasFree) AppPalette.Arcane else AppPalette.TextTertiary,
            )
            Text(
                "${resource.current}/${resource.max}",
                modifier = Modifier.alignByBaseline(),
                style = AppText.caption,
                color = if (hasFree) AppPalette.TextSecondary else AppPalette.RadioIdle,
            )
        }
        Spacer(Modifier.height(10.dp))
        AppCells(total = resource.max, filled = resource.current)
    }
}

@Composable
private fun SlotPanel(
    column: SlotColumn,
    readOnly: Boolean,
    send: (CounterEvent) -> Unit,
) {
    val resource = column.resource
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfacePopover,
                    border = AppPalette.Arcane,
                    borderWidth = 1.5.dp,
                ).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { send(CounterEvent.SlotLevelToggled(column.level)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.me_slots_level_title, column.level.toRomanNumeral()),
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.TextHeading,
            )
            Text(
                stringResource(Res.string.me_slots_free, resource.current, resource.max),
                modifier = Modifier.weight(1f),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
            AppTriangle(pointsUp = true, color = AppPalette.TextMuted, modifier = Modifier.size(10.dp))
        }
        AppCells(
            total = resource.max,
            filled = resource.current,
            size = CellSize.Big,
            onPress = if (readOnly) null else ({ send(CounterEvent.CellPressed(resource.id, it)) }),
        )
        Text(stringResource(Res.string.me_slots_cell_hint), style = AppText.caption, color = AppPalette.TextSecondary)
        Box(
            Modifier
                .fillMaxWidth()
                .dashedBorder(AppPalette.BorderHover, cornerRadius = 12.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.me_slots_list_placeholder),
                style = AppText.footnote,
                color = AppPalette.TextTertiary,
                textAlign = TextAlign.Center,
            )
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
