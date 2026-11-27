package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.domain.model.atRank
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_components
import incadent.shared.generated.resources.me_spells_card_concentration
import incadent.shared.generated.resources.me_spells_card_damage
import incadent.shared.generated.resources.me_spells_card_duration
import incadent.shared.generated.resources.me_spells_card_range
import incadent.shared.generated.resources.me_spells_card_ritual
import incadent.shared.generated.resources.me_spells_card_time
import incadent.shared.generated.resources.me_spells_component_m
import incadent.shared.generated.resources.me_spells_component_s
import incadent.shared.generated.resources.me_spells_component_v
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Badges(spell: CharacterSpell) {
    if (!spell.concentration && !spell.ritual && !spell.hasFreeCasts) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (spell.concentration) {
            Badge("◎ " + stringResource(Res.string.me_spells_card_concentration), AppPalette.GoldBright)
        }
        if (spell.ritual) Badge(stringResource(Res.string.me_spells_card_ritual), AppPalette.Arcane)
        if (spell.hasFreeCasts) Badge(freeMeta().capitalizeFirst(), AppPalette.Arcane)
    }
}

@Composable
private fun Badge(
    text: String,
    color: Color,
) {
    Text(
        text,
        modifier =
            Modifier
                .appCard(
                    background = color.copy(alpha = 0.1f),
                    border = color.copy(alpha = 0.55f),
                ).padding(horizontal = 12.dp, vertical = 6.dp),
        style = semiBold(AppText.footnote),
        color = color,
    )
}

// empty fields are left out so a card of a short spell stays short
@Composable
internal fun CardGrid(
    spell: CharacterSpell,
    onHold: ((SpellBlock) -> Unit)?,
) {
    val cells =
        listOf(
            Cell(SpellBlock.TIME, stringResource(Res.string.me_spells_card_time), castTimeLabel(spell)),
            Cell(SpellBlock.RANGE, stringResource(Res.string.me_spells_card_range), spell.range),
            Cell(
                SpellBlock.COMPONENTS,
                stringResource(Res.string.me_spells_card_components),
                if (spell.hasComponents) "x" else "",
            ),
            Cell(SpellBlock.DURATION, stringResource(Res.string.me_spells_card_duration), durationLabel(spell)),
        ).filter { it.value.isNotBlank() }
    if (cells.isEmpty()) return
    Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.BorderSubtle)) {
        cells.chunked(2).forEachIndexed { index, row ->
            if (index > 0) AppDivider()
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { column, cell ->
                    if (column > 0) VerticalLine()
                    val body = @Composable {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionOverline(cell.label, color = AppPalette.TextTertiary)
                            if (cell.value == COMPONENTS_MARK) {
                                Components(spell)
                            } else {
                                Text(
                                    cell.value,
                                    style = semiBold(MaterialTheme.typography.bodyLarge),
                                    color = AppPalette.TextHeading,
                                )
                            }
                        }
                    }
                    val slot = Modifier.weight(1f).heightIn(min = CellHeight)
                    if (onHold == null) {
                        Box(slot.padding(12.dp)) { body() }
                    } else {
                        HoldToEditField({ onHold(cell.block) }, CellFrame, slot) { body() }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private class Cell(
    val block: SpellBlock,
    val label: String,
    val value: String,
)

private const val COMPONENTS_MARK = "x"
private val CellHeight = 64.dp

// the hold fills the whole cell up to its borders, the grid card clips the corners
private val CellFrame = HoldFrame(0.dp, 0.dp, 12.dp, 10.dp, 0.dp, fillWidth = true)

@Composable
private fun VerticalLine() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(AppPalette.BorderSubtle))
}

// the three letters, a lit one is on the spell and the material is written under them
@Composable
private fun Components(spell: CharacterSpell) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Letter(stringResource(Res.string.me_spells_component_v), spell.verbal)
        Letter(stringResource(Res.string.me_spells_component_s), spell.somatic)
        Letter(stringResource(Res.string.me_spells_component_m), spell.hasMaterial)
    }
    if (spell.hasMaterial &&
        spell.material.isNotBlank()
    ) {
        Text(spell.material, style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun Letter(
    text: String,
    on: Boolean,
) {
    Box(
        Modifier
            .size(LetterSize)
            .appCard(
                shape = LetterShape,
                background = if (on) AppPalette.Arcane.copy(alpha = 0.12f) else Color.Transparent,
                border = if (on) AppPalette.Arcane else AppPalette.BorderHover,
                borderWidth = 1.3.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppText.caption.copy(fontWeight = FontWeight.Bold),
            color = if (on) AppPalette.ArcaneLight else AppPalette.TextTertiary,
        )
    }
}

private val LetterSize = 24.dp
private val LetterShape = RoundedCornerShape(7.dp)

/** the damage with its icons and the caption of its growth, each of the two opens its own sheet on a hold */
@Composable
internal fun DamageBlock(
    spell: CharacterSpell,
    characterLevel: Int,
    onHold: ((SpellBlock) -> Unit)?,
) {
    val at = spell.atRank(characterLevel)
    if (at.damage.none { it.shown() }) return
    Column(Modifier.fillMaxWidth().appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)) {
        Held(SpellBlock.DAMAGE, onHold, DamageFrame) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionOverline(stringResource(Res.string.me_spells_card_damage), color = AppPalette.TextTertiary)
                DamageRows(at.damage, at.projectiles)
            }
        }
    }
}

// a state has no dice, so it is told by its name
private fun SpellDamage.shown() = dice.isNotBlank() || effect is SpellEffect.Condition

private val DamageFrame = HoldFrame(0.dp, 0.dp, 14.dp, 12.dp, 0.dp, fillWidth = true)

/** the content as it is, or inside a hold that reports [block] when the card can be changed */
@Composable
internal fun Held(
    block: SpellBlock,
    onHold: ((SpellBlock) -> Unit)?,
    frame: HoldFrame,
    content: @Composable () -> Unit,
) {
    if (onHold == null) {
        Box(Modifier.padding(horizontal = frame.paddingHorizontal, vertical = frame.paddingVertical)) { content() }
    } else {
        HoldToEditField({ onHold(block) }, frame) { content() }
    }
}

/** the damage as the card writes it, the dice big and the name of the effect small after them */
@Composable
private fun DamageRows(
    damage: List<SpellDamage>,
    projectiles: Int,
) {
    val rows = damage.filter { it.shown() }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Text("+", style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextTertiary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SpellEffectGlyph(row.effect, size = 17.dp)
                val isState = row.effect is SpellEffect.Condition
                val head =
                    when {
                        isState -> effectTitle(row.effect)
                        index == 0 && projectiles > 1 -> "$projectiles × ${row.dice}"
                        else -> row.dice
                    }
                Text(head, style = semiBold(MaterialTheme.typography.bodyLarge), color = AppPalette.TextHeading)
                if (!isState) Text(effectLabel(row.effect), style = AppText.footnote, color = AppPalette.TextSecondary)
            }
        }
    }
}

// the mockup sets values, badges and dice in 600
private fun semiBold(style: TextStyle): TextStyle = style.copy(fontWeight = FontWeight.SemiBold)
