package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_TIERS
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.SpellAt
import com.lyrismet.incadent.feature.codex.domain.model.atCharacterLevel
import com.lyrismet.incadent.feature.codex.domain.model.atCircle
import com.lyrismet.incadent.feature.codex.domain.model.cantripTier
import com.lyrismet.incadent.feature.codex.domain.model.pinnedCircle
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.SpellPay
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_cantrip_note
import incadent.shared.generated.resources.me_spells_card_no_slots_note
import incadent.shared.generated.resources.me_spells_card_ref_circle
import incadent.shared.generated.resources.me_spells_card_ref_level
import incadent.shared.generated.resources.me_spells_card_ref_none
import incadent.shared.generated.resources.me_spells_card_ref_now
import incadent.shared.generated.resources.me_spells_card_ref_pin_hint
import incadent.shared.generated.resources.me_spells_card_ref_pinned
import incadent.shared.generated.resources.me_spells_card_spend_free
import incadent.shared.generated.resources.me_spells_card_spend_free_count
import incadent.shared.generated.resources.me_spells_card_spend_free_note
import incadent.shared.generated.resources.me_spells_card_spend_none_note
import incadent.shared.generated.resources.me_spells_card_spend_slot
import incadent.shared.generated.resources.me_spells_card_spend_uses
import incadent.shared.generated.resources.me_spells_growth_targets_extra
import incadent.shared.generated.resources.me_spells_growth_tier_last
import incadent.shared.generated.resources.me_spells_growth_tier_range
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_CIRCLE = 9
private const val DIMMED = 0.45f
private const val CHIPS_IN_ROW = 3
private const val PIPS_IN_ROW = 2
private val ReferenceKey = 40.dp
private val CantripKey = 44.dp

/** one line of the reference with what the spell comes to on a circle or a level range, [changed] is lit gold */
private class RefRow(
    val key: String,
    val at: SpellAt,
    val changed: Boolean,
    val dimmed: Boolean,
    val note: String?,
    val circle: Int = 0,
) {
    val keyWidth: Dp get() = if (circle > 0) ReferenceKey else CantripKey
}

// a cantrip with growth by level or a spell that grows with the slot and is not paid with its own uses
private val CharacterSpell.hasReference: Boolean
    get() = if (isCantrip) growsWithLevel else growth.enabled && !hasFreeCasts

/** what the spell comes to a circle higher or on each level step, empty for a spell that does not grow */
@Composable
internal fun GrowthReference(
    spell: CharacterSpell,
    state: SpellsBlockState,
    onHold: ((SpellBlock) -> Unit)?,
    onPin: ((Int) -> Unit)? = null,
) {
    val cantrip = spell.isCantrip
    if (!spell.hasReference) return
    val rows = if (cantrip) cantripRows(spell, state.characterLevel) else circleRows(spell, state.columns)
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)) {
        Held(SpellBlock.GROWTH, onHold, ReferenceFrame) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionOverline(
                    stringResource(
                        if (cantrip) Res.string.me_spells_card_ref_level else Res.string.me_spells_card_ref_circle,
                    ),
                    color = AppPalette.TextTertiary,
                )
                if (!cantrip && onPin != null) {
                    Text(
                        stringResource(Res.string.me_spells_card_ref_pin_hint),
                        style = AppText.footnote,
                        color = AppPalette.TextTertiary,
                    )
                }
                rows.forEachIndexed { index, row ->
                    if (index > 0) AppDivider()
                    ReferenceLine(spell, row, if (cantrip) null else onPin)
                }
            }
        }
    }
}

// the hold covers the whole table, a hold on it opens the upcast rules
private val ReferenceFrame = HoldFrame(0.dp, 0.dp, 14.dp, 14.dp, 0.dp, fillWidth = true)

@Composable
private fun cantripRows(
    spell: CharacterSpell,
    characterLevel: Int,
): List<RefRow> {
    val tiers = listOf(1) + CANTRIP_TIERS
    val current = cantripTier(characterLevel)
    val now = stringResource(Res.string.me_spells_card_ref_now, characterLevel)
    return tiers.mapIndexed { index, from ->
        val key =
            if (index == tiers.lastIndex) {
                stringResource(Res.string.me_spells_growth_tier_last, from)
            } else {
                stringResource(Res.string.me_spells_growth_tier_range, from, tiers[index + 1] - 1)
            }
        RefRow(key, spell.atCharacterLevel(from), index == current, false, now.takeIf { index == current })
    }
}

@Composable
private fun circleRows(
    spell: CharacterSpell,
    columns: List<SlotColumn>,
): List<RefRow> {
    val top = columns.filter { it.resource.max > 0 }.maxOfOrNull { it.level } ?: 0
    val last = maxOf(spell.level + 1, top + 1).coerceAtMost(MAX_CIRCLE)
    val none = stringResource(Res.string.me_spells_card_ref_none)
    val pinned = stringResource(Res.string.me_spells_card_ref_pinned)
    var before: SpellAt? = null
    return (spell.level..last).map { circle ->
        val at = spell.atCircle(circle)
        val has = columns.any { it.level == circle && it.resource.max > 0 }
        val row =
            RefRow(
                circle.toRomanNumeral(),
                at,
                before != null && at != before,
                !has,
                if (spell.pinnedCircle == circle) pinned else none.takeIf { !has },
                circle,
            )
        before = at
        row
    }
}

@Composable
private fun ReferenceLine(
    spell: CharacterSpell,
    row: RefRow,
    onPin: ((Int) -> Unit)?,
) {
    val pinned = spell.pinnedCircle == row.circle
    val lit = row.changed || pinned || (row.note != null && spell.isCantrip)
    val color = if (lit) AppPalette.GoldBright else AppPalette.TextPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .alpha(if (row.dimmed && !pinned) DIMMED else 1f)
            .then(if (onPin != null) Modifier.clickable { onPin(if (pinned) 0 else row.circle) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            row.key,
            Modifier.width(row.keyWidth),
            style = MaterialTheme.typography.titleMedium,
            color = if (spell.isCantrip && row.changed) AppPalette.GoldBright else AppPalette.Arcane,
        )
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            DamageLine(row.at.damage, row.at.projectiles, MaterialTheme.typography.bodyMedium, color = color)
            Extras(spell, row.at, color)
        }
        row.note?.let { Text(it, style = AppText.footnote, color = AppPalette.TextSecondary) }
    }
}

/** the duration, the extra targets, the range and the notes that the growth changes */
@Composable
private fun Extras(
    spell: CharacterSpell,
    at: SpellAt,
    color: Color,
) {
    val rules = spell.growth.rules
    val style = MaterialTheme.typography.bodyMedium
    if (rules.any { it.target == GrowthTarget.DURATION }) {
        val mark = if (spell.concentration) "◎ " else ""
        Text(mark + durationLabel(at.durationUnit, at.durationValue), style = style, color = color)
    }
    if (rules.any { it.target == GrowthTarget.TARGETS } && at.extraTargets > 0) {
        Text(
            pluralStringResource(Res.plurals.me_spells_growth_targets_extra, at.extraTargets, at.extraTargets),
            style = style,
            color = color,
        )
    }
    if (rules.any { it.target == GrowthTarget.RANGE }) Text(at.range, style = style, color = color)
    at.notes.forEach { Text(it, style = style, color = color) }
}

/** the circles a spell can be cast with, a press spends one and the toast takes it back */
@Composable
internal fun SpendChips(
    spell: CharacterSpell,
    columns: List<SlotColumn>,
    send: (SpellsEvent) -> Unit,
) {
    if (spell.isCantrip) {
        Text(
            stringResource(Res.string.me_spells_card_cantrip_note),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        return
    }
    val options = columns.filter { it.level >= spell.level && it.resource.max > 0 }.sortedBy { it.level }
    if (spell.hasFreeCasts) {
        SpendRow(
            title = stringResource(Res.string.me_spells_card_spend_uses),
            chips =
                listOf(
                    Chip(
                        stringResource(Res.string.me_spells_card_spend_free),
                        spell.freeLeft,
                        spell.freeMax,
                        counter =
                            stringResource(
                                Res.string.me_spells_card_spend_free_count,
                                spell.freeLeft,
                                spell.freeMax,
                            ),
                    ) {
                        send(SpellsEvent.Cast(spell.id, SpellPay.Free))
                    },
                ),
            note = stringResource(Res.string.me_spells_card_spend_free_note, spell.freeLeft, spell.freeMax),
        )
        return
    }
    val none = options.none { it.resource.current > 0 }
    SpendRow(
        title = stringResource(Res.string.me_spells_card_spend_slot),
        chips =
            options.map {
                Chip(it.level.toRomanNumeral(), it.resource.current, it.resource.max) {
                    send(SpellsEvent.Cast(spell.id, SpellPay.Slot(it.level)))
                }
            },
        note =
            when {
                options.isEmpty() ->
                    stringResource(
                        Res.string.me_spells_card_no_slots_note,
                        spell.level.toRomanNumeral(),
                    )
                none -> stringResource(Res.string.me_spells_card_spend_none_note, spell.level.toRomanNumeral())
                else -> null
            },
    )
}

private class Chip(
    val label: String,
    val free: Int,
    val max: Int,
    // the count in words when the chip has no row of dots
    val counter: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun SpendRow(
    title: String,
    chips: List<Chip>,
    note: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(title, color = AppPalette.TextTertiary)
        // a row holds three chips with empty places kept so every chip has one width and a lone chip takes the row
        val perRow = if (chips.size == 1) 1 else CHIPS_IN_ROW
        chips.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { SpendChip(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        note?.let { Text(it, style = AppText.footnote, color = AppPalette.TextSecondary) }
    }
}

@Composable
private fun SpendChip(
    chip: Chip,
    modifier: Modifier,
) {
    val enabled = chip.free > 0
    Column(
        modifier
            .heightIn(min = 58.dp)
            .alpha(if (enabled) 1f else DIMMED)
            .appCard(
                AppShapes.Field,
                AppPalette.Background,
                if (enabled) AppPalette.Arcane.copy(alpha = 0.45f) else AppPalette.BorderSubtle,
                onClick = if (enabled) chip.onClick else null,
            ).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(chip.label, style = MaterialTheme.typography.titleMedium, color = AppPalette.ArcaneLight, maxLines = 1)
        if (chip.counter != null) {
            Text(chip.counter, style = AppText.footnote, color = AppPalette.TextSecondary)
        } else {
            // rows of fixed size, a wrapping row inside the chip lost its second line to the intrinsic height
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                remember(chip.max) { List(chip.max.coerceAtLeast(1)) { it }.chunked(PIPS_IN_ROW) }.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { index ->
                            val lit = index < chip.free
                            Dot(
                                size = 8.dp,
                                color = if (lit) AppPalette.Arcane else Color.Transparent,
                                borderColor = if (lit) null else AppPalette.TextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}
