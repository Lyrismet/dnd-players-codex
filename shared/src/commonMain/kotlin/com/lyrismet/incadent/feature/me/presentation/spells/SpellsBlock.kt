package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppCells
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
import com.lyrismet.incadent.core.designsystem.component.AppTriangle
import com.lyrismet.incadent.core.designsystem.component.CellEffect
import com.lyrismet.incadent.core.designsystem.component.CellSize
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.rememberSparkClock
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_LEVEL
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.spellLevels
import com.lyrismet.incadent.feature.me.domain.spellsOfLevel
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
import incadent.shared.generated.resources.me_spells_add
import incadent.shared.generated.resources.me_spells_cantrips_panel_sub
import incadent.shared.generated.resources.me_spells_cantrips_sub
import incadent.shared.generated.resources.me_spells_cantrips_title
import incadent.shared.generated.resources.me_spells_list_empty
import incadent.shared.generated.resources.me_spells_slots_title
import incadent.shared.generated.resources.me_spells_tile_ability
import incadent.shared.generated.resources.me_spells_tile_attack
import incadent.shared.generated.resources.me_spells_tile_dc
import org.jetbrains.compose.resources.stringResource

/** the spell numbers, the slot tiles with the open level's list: one block for the sheet and for the fight */
@Composable
fun SpellsBlock(
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // one clock for every cell of the block, and none when no free cell would move
    val moving = state.columns.any { it.resource.current > 0 && CellEffect.forLevel(it.level) != CellEffect.None }
    val clock = if (moving) rememberSparkClock() else null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpellNumbers(state, send)
        val levels = remember(state.spells, state.columns) { spellLevels(state.spells, state.columns) }
        if (levels.isNotEmpty()) {
            SectionCaption(
                stringResource(Res.string.me_spells_slots_title),
                helpTitle = stringResource(Res.string.me_help_slots_title),
                helpBody = stringResource(Res.string.me_help_slots_body),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                levels.chunked(SLOT_TILES_IN_ROW).forEach { SlotRow(it, state, clock, send) }
            }
        }
        if (!state.readOnly && state.openLevel == null) {
            SpellAddTile(stringResource(Res.string.me_spells_add)) {
                send(
                    SpellsEvent.Add(
                        levels.firstOrNull { it > CANTRIP_LEVEL } ?: CANTRIP_LEVEL,
                    ),
                )
            }
        }
    }
    SpellSheets(state, send)
}

private const val SLOT_TILES_IN_ROW = 3

@Composable
private fun SpellNumbers(
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val editable = state.tilesEditable && !state.readOnly
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumberTile(
            value = state.ability?.let { stringResource(it.abbrRes()) }.orDash(),
            label = stringResource(Res.string.me_spells_tile_ability),
            help =
                stringResource(Res.string.me_help_spell_ability_title) to
                    stringResource(Res.string.me_help_spell_ability_body),
            modifier = Modifier.weight(1f),
            onClick = if (editable) ({ send(SpellsEvent.TileTapped(SpellTile.ABILITY)) }) else null,
            dropdown = editable,
        )
        NumberTile(
            value = state.saveDc?.toString().orDash(),
            label = stringResource(Res.string.me_spells_tile_dc),
            help =
                stringResource(
                    Res.string.me_help_spell_dc_title,
                ) to stringResource(Res.string.me_help_spell_dc_body),
            modifier = Modifier.weight(1f),
            onClick = if (editable && state.saveDc != null) ({ send(SpellsEvent.TileTapped(SpellTile.DC)) }) else null,
        )
        NumberTile(
            value = state.attack.signed(),
            label = stringResource(Res.string.me_spells_tile_attack),
            help =
                stringResource(Res.string.me_help_spell_attack_title) to
                    stringResource(Res.string.me_help_spell_attack_body),
            modifier = Modifier.weight(1f),
            onClick =
                if (editable &&
                    state.attack != null
                ) {
                    ({ send(SpellsEvent.TileTapped(SpellTile.ATTACK)) })
                } else {
                    null
                },
        )
    }
}

@Composable
private fun NumberTile(
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
            AppHelpHint(help.first, help.second, modifier = Modifier.size(28.dp))
        }
    }
}

/** a row of up to three level tiles, the open level shows its cells and spells in a panel under the row */
@Composable
private fun SlotRow(
    row: List<Int>,
    state: SpellsBlockState,
    clock: State<Float>?,
    send: (SpellsEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { level ->
                LevelTile(
                    level,
                    state.columns.firstOrNull { it.level == level },
                    level == state.openLevel,
                    clock,
                    state.spells.count { it.isCantrip },
                    Modifier.weight(1f),
                ) { send(SpellsEvent.LevelToggled(level)) }
            }
            repeat(SLOT_TILES_IN_ROW - row.size) { Spacer(Modifier.weight(1f)) }
        }
        row.firstOrNull { it == state.openLevel }?.let { LevelPanel(it, state, clock, send) }
    }
}

@Composable
private fun LevelTile(
    level: Int,
    column: SlotColumn?,
    open: Boolean,
    clock: State<Float>?,
    cantripCount: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val cantrips = level == CANTRIP_LEVEL
    val resource = column?.resource
    val hasFree = cantrips || (resource?.current ?: 0) > 0
    Column(
        modifier =
            modifier
                .heightIn(min = 68.dp)
                .fillMaxHeight()
                .appCard(
                    background = if (open) AppPalette.SurfacePopover else AppPalette.Surface,
                    border = if (open) AppPalette.Arcane else AppPalette.BorderSubtle,
                    borderWidth = if (open) 1.5.dp else 1.dp,
                    onClick = onClick,
                ).padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (cantrips) {
            CantripsTileHead(cantripCount)
        } else {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    level.toRomanNumeral(),
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (hasFree) AppPalette.Arcane else AppPalette.TextTertiary,
                )
                Text(
                    "${resource?.current ?: 0}/${resource?.max ?: 0}",
                    modifier = Modifier.alignByBaseline(),
                    style = AppText.caption,
                    color = if (hasFree) AppPalette.TextSecondary else AppPalette.RadioIdle,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (resource != null) {
                AppCells(
                    total = resource.max,
                    filled = resource.current,
                    effect = CellEffect.forLevel(level),
                    clock = clock,
                    variationSeed = level,
                )
            }
        }
    }
}

// the cantrip tile reads like a level, the infinity stands where the cells would be
@Composable
private fun CantripsTileHead(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.me_spells_cantrips_title),
                modifier = Modifier.weight(1f).alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = AppPalette.Arcane,
                maxLines = 1,
            )
            Text(
                count.toString(),
                Modifier.alignByBaseline(),
                style = AppText.caption,
                color = AppPalette.TextSecondary,
            )
        }
        Text(
            stringResource(Res.string.me_spells_cantrips_sub),
            style = AppText.caption,
            color = AppPalette.TextSecondary,
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.height(13.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            "∞",
            modifier = Modifier.wrapContentHeight(unbounded = true),
            style = TextStyle(fontSize = 18.sp, lineHeight = 18.sp),
            color = AppPalette.Arcane,
        )
    }
}

@Composable
private fun LevelPanel(
    level: Int,
    state: SpellsBlockState,
    clock: State<Float>?,
    send: (SpellsEvent) -> Unit,
) {
    val resource = state.columns.firstOrNull { it.level == level }?.resource
    val list = remember(state.spells, level) { spellsOfLevel(state.spells, level) }
    // the panel scrolls into view whole when its level opens, in the sheet and in the fight alike
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(level) { requester.bringIntoView() }
    Column(
        modifier =
            Modifier
                .bringIntoViewRequester(requester)
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfacePopover,
                    border = AppPalette.Arcane,
                    borderWidth = 1.5.dp,
                ).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PanelHeader(level, resource, send)
        if (resource != null) {
            AppCells(
                total = resource.max,
                filled = resource.current,
                size = CellSize.Big,
                onPress = if (state.readOnly) null else ({ send(SpellsEvent.CellPressed(resource.id, it)) }),
                effect = CellEffect.forLevel(level),
                clock = clock,
                variationSeed = level,
            )
            Text(
                stringResource(Res.string.me_slots_cell_hint),
                style = AppText.caption,
                color = AppPalette.TextSecondary,
            )
        }
        if (list.isEmpty()) {
            EmptyList()
        } else {
            SpellList(list, state.characterLevel, send)
        }
        if (!state.readOnly) {
            SpellAddTile(stringResource(Res.string.me_spells_add)) { send(SpellsEvent.Add(level)) }
        }
    }
}

@Composable
private fun PanelHeader(
    level: Int,
    resource: CharacterResource?,
    send: (SpellsEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { send(SpellsEvent.LevelToggled(level)) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (level == CANTRIP_LEVEL) {
                stringResource(Res.string.me_spells_cantrips_title)
            } else {
                stringResource(Res.string.me_slots_level_title, level.toRomanNumeral())
            },
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.TextHeading,
        )
        Text(
            when {
                resource != null -> stringResource(Res.string.me_slots_free, resource.current, resource.max)
                level == CANTRIP_LEVEL -> stringResource(Res.string.me_spells_cantrips_panel_sub)
                else -> ""
            },
            modifier = Modifier.weight(1f),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        AppTriangle(pointsUp = true, color = AppPalette.TextMuted, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun EmptyList() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.me_spells_list_empty),
            style = AppText.footnote,
            color = AppPalette.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}
